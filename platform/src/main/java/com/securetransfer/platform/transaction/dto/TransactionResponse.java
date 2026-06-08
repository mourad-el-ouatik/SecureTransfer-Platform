package com.securetransfer.platform.transaction.dto;

import com.securetransfer.platform.transaction.entity.TransactionStatus;
import com.securetransfer.platform.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        Long senderId,
        Long receiverId,
        BigDecimal amount,
        BigDecimal fee,
        TransactionStatus status,
        TransactionType type,
        String withdrawalCode,
        LocalDateTime withdrawalExpiresAt,
        LocalDateTime createdAt) {
}
