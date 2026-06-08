package com.securetransfer.platform.transaction.fraud;

public sealed interface FraudResult {
    record Clean() implements FraudResult {
    }

    record Suspicious(String reason, double score) implements FraudResult {
    }

    record Blocked(String reason) implements FraudResult {
    }
}
