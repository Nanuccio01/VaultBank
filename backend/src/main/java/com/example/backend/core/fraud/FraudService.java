package com.example.backend.core.fraud;

import com.example.backend.core.transfer.TransferRepository;
import com.example.backend.core.user.UserEntity;
import com.example.backend.core.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FraudService {

    private final TransferRepository transferRepository;
    private final UserRepository userRepository;

    private final ZoneId zone = ZoneId.of("Europe/Rome");
    private final long lockSeconds;

    public FraudService(TransferRepository transferRepository,
                        UserRepository userRepository,
                        @Value("${vaultbank.fraud.lock-seconds:180}") long lockSeconds) {
        this.transferRepository = transferRepository;
        this.userRepository = userRepository;
        this.lockSeconds = lockSeconds;
    }

    public FraudDecision evaluate(UUID userId, String toIban, BigDecimal amount, Instant nowUtc) {
        List<String> reasons = new ArrayList<>();
        int score = 0;

        if (amount.compareTo(new BigDecimal("5000.00")) >= 0) {
            score += 60;
            reasons.add("High amount");
        } else if (amount.compareTo(new BigDecimal("1500.00")) >= 0) {
            score += 35;
            reasons.add("Unusually high amount");
        }

        int hour = ZonedDateTime.ofInstant(nowUtc, zone).getHour();
        if (hour >= 0 && hour < 6) {
            score += 25;
            reasons.add("Unusual time (night hours)");
        }

        long last60s = transferRepository.countByFromUserIdAndCreatedAtAfter(userId, nowUtc.minusSeconds(60));
        if (last60s >= 5) {
            score += 80;
            reasons.add("High transfer velocity");
        } else if (last60s >= 3) {
            score += 40;
            reasons.add("Unusual transfer velocity");
        }

        boolean knownBeneficiary = transferRepository.existsByFromUserIdAndToIban(userId, toIban);
        if (!knownBeneficiary && amount.compareTo(new BigDecimal("500.00")) >= 0) {
            score += 30;
            reasons.add("New beneficiary with medium/high amount");
        }

        if (score >= 90 || amount.compareTo(new BigDecimal("10000.00")) >= 0) {
            return new FraudDecision(FraudDecision.Action.BLOCK, score, reasons);
        }
        if (score >= 45) {
            return new FraudDecision(FraudDecision.Action.STEP_UP, score, reasons);
        }
        return new FraudDecision(FraudDecision.Action.ALLOW, score, reasons);
    }

    public record LockStatus(boolean locked, Instant lockedUntil, long retryAfterSeconds) {}

    @Transactional
    public LockStatus checkAndClearLock(UUID userId, Instant now) {
        UserEntity u = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Instant until = u.getLockedUntil();
        if (until == null) return new LockStatus(false, null, 0);

        if (until.isAfter(now)) {
            long retry = Duration.between(now, until).getSeconds();
            return new LockStatus(true, until, Math.max(0, retry));
        }

        // lock expired -> clear automatically
        u.setLockedUntil(null);
        u.setLockReason(null);
        userRepository.save(u);
        return new LockStatus(false, null, 0);
    }

    @Transactional
    public Instant applyTemporaryLock(UUID userId, Instant now, List<String> reasons) {
        UserEntity u = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Instant until = now.plusSeconds(lockSeconds);
        u.setLockedUntil(until);

        String msg = String.join("; ", reasons);
        if (msg.length() > 200) msg = msg.substring(0, 200);
        u.setLockReason(msg);

        userRepository.save(u);
        return until;
    }
}
