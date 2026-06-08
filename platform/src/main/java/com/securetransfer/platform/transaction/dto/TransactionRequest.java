package com.securetransfer.platform.transaction.dto;

import com.securetransfer.platform.transaction.entity.TransactionType;

import java.math.BigDecimal;

public record TransactionRequest(
        Long receiverId,
        BigDecimal amount,
        TransactionType type,
        String idempotencyKey) {
}
