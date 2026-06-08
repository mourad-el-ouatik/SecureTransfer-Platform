package com.securetransfer.platform.transaction.fraud;

import com.securetransfer.platform.transaction.repository.TransactionRepository;
import com.securetransfer.platform.user.dto.UserResponse;
import com.securetransfer.platform.user.entity.KycStatus;
import com.securetransfer.platform.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private final UserService userService;
    private final TransactionRepository transactionRepository;

    public FraudResult analyze(Long senderId, BigDecimal amount) {
        UserResponse sender = userService.getUser(senderId);

        if (amount.compareTo(new BigDecimal("50000")) > 0) {
            log.warn("Fraud detected: blocked - Amount {} exceeds absolute limit", amount);
            return new FraudResult.Blocked("Amount exceeds absolute maximum allowed limit (50000)");
        }

        if (sender.kycStatus() == KycStatus.PENDING) {
            log.warn("Fraud detected: suspicious - Sender KYC is pending");
            return new FraudResult.Suspicious("Sender KYC is still pending", 0.7);
        }

        if (sender.kycStatus() == KycStatus.REJECTED) {
            log.warn("Fraud detected: blocked - Sender KYC is rejected");
            return new FraudResult.Blocked("Sender KYC is rejected");
        }

        BigDecimal singleLimit = sender.singleTransactionLimit();
        if (singleLimit != null &&
                amount.compareTo(singleLimit.multiply(new BigDecimal("0.99"))) >= 0 &&
                amount.compareTo(singleLimit) <= 0) {
            log.warn("Fraud detected: suspicious - Testing limits");
            return new FraudResult.Suspicious("Amount is suspiciously close to the exact transaction limit", 0.6);
        }

        long recentTransactions = transactionRepository.countBySenderIdAndCreatedAtAfter(
                senderId, LocalDateTime.now().minusHours(1));
        if (recentTransactions >= 5) {
            log.warn("Fraud detected: suspicious - High velocity");
            return new FraudResult.Suspicious(
                    "High velocity: user made " + recentTransactions + " transactions in the last hour", 0.9);
        }

        return new FraudResult.Clean();
    }
}
