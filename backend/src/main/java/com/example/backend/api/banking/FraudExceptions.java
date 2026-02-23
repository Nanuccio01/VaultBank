package com.example.backend.api.banking;

import java.time.Instant;
import java.util.List;

public class FraudExceptions {

    public static class FraudStepUpRequiredException extends RuntimeException {
        private final List<String> reasons;
        public FraudStepUpRequiredException(List<String> reasons) {
            super("Step-up required");
            this.reasons = reasons;
        }
        public List<String> getReasons() { return reasons; }
    }

    public static class FraudBlockedException extends RuntimeException {
        private final List<String> reasons;
        private final Instant lockedUntil;

        public FraudBlockedException(List<String> reasons, Instant lockedUntil) {
            super("Transfer blocked by antifraud");
            this.reasons = reasons;
            this.lockedUntil = lockedUntil;
        }

        public List<String> getReasons() { return reasons; }
        public Instant getLockedUntil() { return lockedUntil; }
    }
}
