// UserResponse.java — Ce qu'on renvoie au client (PAS de CIN ni de phone en clair !)
package com.securetransfer.platform.user.dto;

import com.securetransfer.platform.user.entity.KycStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        KycStatus kycStatus,
        BigDecimal dailyTransactionLimit,
        BigDecimal singleTransactionLimit,
        LocalDateTime createdAt
) {}
