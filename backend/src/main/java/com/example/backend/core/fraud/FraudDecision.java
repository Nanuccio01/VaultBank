package com.example.backend.core.fraud;

import java.util.List;

public record FraudDecision(Action action, int riskScore, List<String> reasons) {
    public enum Action { ALLOW, STEP_UP, BLOCK }
}