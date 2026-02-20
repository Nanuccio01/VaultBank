package com.example.backend.core.banking;

import com.example.backend.core.crypto.CryptoService;
import com.example.backend.core.transfer.TransferEntity;
import com.example.backend.core.transfer.TransferRepository;
import com.example.backend.core.user.UserEntity;
import com.example.backend.core.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BankingService {

    private final UserRepository userRepository;
    private final TransferRepository transferRepository;
    private final CryptoService cryptoService;

    public BankingService(UserRepository userRepository,
                          TransferRepository transferRepository,
                          CryptoService cryptoService) {
        this.userRepository = userRepository;
        this.transferRepository = transferRepository;
        this.cryptoService = cryptoService;
    }

    @Transactional
    public TransferResult transfer(UUID fromUserId, String toIban, BigDecimal amount, String causal) {
        BigDecimal normalized = normalizeAmount(amount);

        // Load sender (lock)
        UserEntity sender = userRepository.findByIdForUpdate(fromUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (sender.getIban() != null && sender.getIban().equalsIgnoreCase(toIban)) {
            throw new IllegalArgumentException("Cannot transfer to your own IBAN");
        }

        // Check if recipient exists in our DB (internal transfer)
        Optional<UserEntity> recipientOpt = userRepository.findByIban(toIban);

        UUID toUserId = recipientOpt.map(UserEntity::getId).orElse(null);

        if (toUserId != null) {
            // Lock both users in stable order to avoid deadlocks
            UUID a = fromUserId.compareTo(toUserId) <= 0 ? fromUserId : toUserId;
            UUID b = fromUserId.compareTo(toUserId) <= 0 ? toUserId : fromUserId;

            UserEntity first = userRepository.findByIdForUpdate(a).orElseThrow(() -> new IllegalArgumentException("User not found"));
            UserEntity second = userRepository.findByIdForUpdate(b).orElseThrow(() -> new IllegalArgumentException("User not found"));

            UserEntity lockedSender = fromUserId.equals(first.getId()) ? first : second;
            UserEntity lockedRecipient = toUserId.equals(first.getId()) ? first : second;

            BigDecimal senderBalance = requireBalance(lockedSender);
            if (senderBalance.compareTo(normalized) < 0) throw new IllegalArgumentException("Insufficient funds");

            BigDecimal recipientBalance = requireBalance(lockedRecipient);

            BigDecimal newSenderBalance = senderBalance.subtract(normalized).setScale(2, RoundingMode.HALF_UP);
            BigDecimal newRecipientBalance = recipientBalance.add(normalized).setScale(2, RoundingMode.HALF_UP);

            lockedSender.setBalanceEnc(cryptoService.encryptBigDecimal(newSenderBalance));
            lockedRecipient.setBalanceEnc(cryptoService.encryptBigDecimal(newRecipientBalance));

            userRepository.save(lockedSender);
            userRepository.save(lockedRecipient);

            TransferEntity t = TransferEntity.create(fromUserId, toUserId, lockedSender.getIban(), toIban, normalizeCausal(causal), normalized);
            transferRepository.save(t);

            return new TransferResult(t.getId(), t.getCreatedAt(), newSenderBalance);
        }

        // External transfer: debit sender only
        BigDecimal senderBalance = requireBalance(sender);
        if (senderBalance.compareTo(normalized) < 0) throw new IllegalArgumentException("Insufficient funds");

        BigDecimal newSenderBalance = senderBalance.subtract(normalized).setScale(2, RoundingMode.HALF_UP);
        sender.setBalanceEnc(cryptoService.encryptBigDecimal(newSenderBalance));
        userRepository.save(sender);

        TransferEntity t = TransferEntity.create(fromUserId, null, sender.getIban(), toIban, normalizeCausal(causal), normalized);
        transferRepository.save(t);

        return new TransferResult(t.getId(), t.getCreatedAt(), newSenderBalance);
    }

    @Transactional(readOnly = true)
    public List<MovementItem> latestMovements(UUID userId) {
        List<TransferEntity> transfers = transferRepository.findLatestMovements(userId, PageRequest.of(0, 10));

        // Load involved users in batch (avoid N+1)
        Set<UUID> ids = transfers.stream()
                .flatMap(t -> Arrays.stream(new UUID[]{t.getFromUserId(), t.getToUserId()}))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, UserEntity> users = userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(UserEntity::getId, u -> u));

        return transfers.stream().map(t -> {
            boolean outgoing = userId.equals(t.getFromUserId());

            UserEntity fromU = users.get(t.getFromUserId());
            UserEntity toU = t.getToUserId() != null ? users.get(t.getToUserId()) : null;

            String senderName = fromU != null ? safeName(fromU) : "Unknown";
            String senderIban = t.getFromIban();

            String recipientName = toU != null ? safeName(toU) : "External";
            String recipientIban = t.getToIban();

            return new MovementItem(
                    t.getId(),
                    outgoing ? "OUT" : "IN",
                    t.getAmount(),
                    t.getCausal(),
                    t.getCreatedAt(),
                    senderName,
                    senderIban,
                    recipientName,
                    recipientIban
            );
        }).toList();
    }

    private BigDecimal requireBalance(UserEntity u) {
        BigDecimal b = cryptoService.decryptBigDecimal(u.getBalanceEnc());
        if (b == null) throw new IllegalStateException("Balance not initialized");
        return b;
    }

    private static BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) throw new IllegalArgumentException("Amount is required");
        BigDecimal a = amount.setScale(2, RoundingMode.HALF_UP);
        if (a.compareTo(new BigDecimal("0.01")) < 0) throw new IllegalArgumentException("Amount must be >= 0.01");
        return a;
    }

    private static String normalizeCausal(String causal) {
        if (causal == null) return "Bonifico";
        String c = causal.trim();
        if (c.isEmpty()) return "Bonifico";
        if (c.length() > 140) throw new IllegalArgumentException("Causal too long");
        return c;
    }

    private String safeName(UserEntity u) {
        String fn = cryptoService.decryptString(u.getFirstNameEnc());
        String ln = cryptoService.decryptString(u.getLastNameEnc());
        String s = ((fn == null ? "" : fn) + " " + (ln == null ? "" : ln)).trim();
        return s.isEmpty() ? u.getEmail() : s;
    }

    public record TransferResult(UUID transferId, Instant createdAt, BigDecimal newBalance) {}

    public record MovementItem(
            UUID id,
            String direction,       // IN / OUT
            BigDecimal amount,
            String causal,
            Instant createdAt,
            String senderName,
            String senderIban,
            String recipientName,
            String recipientIban
    ) {}
}
