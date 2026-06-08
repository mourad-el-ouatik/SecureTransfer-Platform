package com.securetransfer.platform.transaction.security;

import com.securetransfer.platform.transaction.entity.Transaction;
import com.securetransfer.platform.transaction.repository.TransactionRepository;
import com.securetransfer.platform.user.dto.UserResponse;
import com.securetransfer.platform.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("transactionSecurity")
@RequiredArgsConstructor
public class TransactionSecurityBean {

    private final TransactionRepository transactionRepository;
    private final UserService userService;

    public boolean isOwner(Long transactionId, String email) {
        UserResponse user = userService.getUserByEmail(email);
        Transaction transaction = transactionRepository.findById(transactionId).orElse(null);
        if (transaction == null) {
            return false;
        }
        return transaction.getSenderId().equals(user.id()) || transaction.getReceiverId().equals(user.id());
    }
}
