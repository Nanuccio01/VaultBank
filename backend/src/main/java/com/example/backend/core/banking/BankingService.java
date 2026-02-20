package com.example.backend.core.banking;

import com.example.backend.core.crypto.CryptoService;
import com.example.backend.core.transfer.TransferEntity;
import com.example.backend.core.transfer.TransferRepository;
import com.example.backend.core.user.UserEntity;
import com.example.backend.core.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

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
    public TransferResult transfer(UUID fromUserId, String toIban, BigDecimal amount) {

        BigDecimal normalized = normalizeAmount(amount);

        UserEntity user = userRepository.findByIdForUpdate(fromUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        BigDecimal balance = cryptoService.decryptBigDecimal(user.getBalanceEnc());
        if (balance == null) {
            throw new IllegalStateException("Balance not initialized");
        }

        if (balance.compareTo(normalized) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        BigDecimal newBalance = balance.subtract(normalized).setScale(2, RoundingMode.HALF_UP);
        user.setBalanceEnc(cryptoService.encryptBigDecimal(newBalance));
        userRepository.save(user);

        TransferEntity t = TransferEntity.create(fromUserId, toIban, normalized);
        transferRepository.save(t);

        return new TransferResult(t.getId(), t.getCreatedAt(), newBalance);
    }

    @Transactional(readOnly = true)
    public List<TransferEntity> latestTransfers(UUID fromUserId) {
        return transferRepository.findTop10ByFromUserIdOrderByCreatedAtDesc(fromUserId);
    }

    private static BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) throw new IllegalArgumentException("Amount is required");
        BigDecimal a = amount.setScale(2, RoundingMode.HALF_UP);
        if (a.compareTo(new BigDecimal("0.01")) < 0) {
            throw new IllegalArgumentException("Amount must be >= 0.01");
        }
        return a;
    }

    public record TransferResult(UUID transferId, java.time.Instant createdAt, BigDecimal newBalance) {}
}
