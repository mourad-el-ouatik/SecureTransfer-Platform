package com.securetransfer.platform.transaction.service;

import com.securetransfer.platform.common.exception.BusinessException;
import com.securetransfer.platform.transaction.entity.FraudAlert;
import com.securetransfer.platform.transaction.entity.Transaction;
import com.securetransfer.platform.transaction.entity.TransactionStatus;
import com.securetransfer.platform.transaction.entity.TransactionType;
import com.securetransfer.platform.transaction.event.TransactionCreatedEvent;
import com.securetransfer.platform.transaction.fraud.FraudDetectionService;
import com.securetransfer.platform.transaction.fraud.FraudResult;
import com.securetransfer.platform.transaction.repository.FraudAlertRepository;
import com.securetransfer.platform.transaction.repository.TransactionRepository;
import com.securetransfer.platform.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final UserService userService;
    private final FraudDetectionService fraudDetectionService;
    private final FeeCalculationService feeCalculationService;
    private final ApplicationEventPublisher eventPublisher;

    private static final String HMAC_SECRET = "Secr3tW!thdr4w4lK3y2026!";

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Transaction processTransfer(Long senderId, Long receiverId, BigDecimal amount, String idempotencyKey,
            TransactionType type) {

        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent request detected for key: {}", idempotencyKey);
            return existing.get();
        }

        userService.validateTransactionLimit(senderId, amount);

        FraudResult fraudResult = fraudDetectionService.analyze(senderId, amount);

        switch (fraudResult) {
            case FraudResult.Blocked b -> {
                log.warn("Transaction blocked: {}", b.reason());
                throw new BusinessException("Transaction blocked: " + b.reason());
            }
            case FraudResult.Clean c -> log.info("Transaction is clean");
            case FraudResult.Suspicious s -> log.warn("Transaction is suspicious: {}", s.reason());
        }

        BigDecimal fee = feeCalculationService.calculateFee(senderId, amount);

        Transaction transaction = new Transaction();
        transaction.setSenderId(senderId);
        transaction.setReceiverId(receiverId);
        transaction.setAmount(amount);
        transaction.setFee(fee);
        transaction.setType(type);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setIdempotencyKey(idempotencyKey);

        if (type == TransactionType.WITHDRAWAL) {
            String code = generateHmacSha256(senderId + ":" + receiverId + ":" + idempotencyKey);
            transaction.setWithdrawalCode(code);
            transaction.setWithdrawalExpiresAt(LocalDateTime.now().plusHours(72));
        }

        Transaction saved = transactionRepository.save(transaction);

        if (fraudResult instanceof FraudResult.Suspicious s) {
            FraudAlert alert = new FraudAlert();
            alert.setTransaction(saved);
            alert.setAlertType("SUSPICIOUS_TRANSACTION");
            alert.setDescription(s.reason());
            alert.setSeverity("HIGH");
            fraudAlertRepository.save(alert);
        }

        eventPublisher.publishEvent(new TransactionCreatedEvent(this, saved));

        return saved;
    }

    private String generateHmacSha256(String data) {
        try {
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(HMAC_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256HMAC.init(secretKey);
            byte[] bytes = sha256HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, 8);
        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC", e);
        }
    }

    @Transactional(readOnly = true)
    public Transaction getTransaction(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Transaction non trouvée : " + id));
    }
}
