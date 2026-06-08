package com.securetransfer.platform.transaction.service;

import com.securetransfer.platform.common.exception.BusinessException;
import com.securetransfer.platform.transaction.entity.Transaction;
import com.securetransfer.platform.transaction.entity.TransactionStatus;
import com.securetransfer.platform.transaction.entity.TransactionType;
import com.securetransfer.platform.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalService {

    private final TransactionRepository transactionRepository;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Transaction processWithdrawal(Long transactionId, String withdrawalCode) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException("Transaction non trouvée"));

        if (transaction.getType() != TransactionType.WITHDRAWAL) {
            throw new BusinessException("Cette transaction n'est pas un retrait en agence.");
        }

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessException(
                    "La transaction n'est pas en attente (Status: " + transaction.getStatus() + ")");
        }

        if (transaction.getWithdrawalExpiresAt() != null
                && LocalDateTime.now().isAfter(transaction.getWithdrawalExpiresAt())) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new BusinessException("Le code de retrait a expiré.");
        }

        if (withdrawalCode == null || !withdrawalCode.equals(transaction.getWithdrawalCode())) {
            throw new BusinessException("Le code de retrait est invalide.");
        }

        transaction.setStatus(TransactionStatus.COMPLETED);

        return transactionRepository.save(transaction);
    }
}
