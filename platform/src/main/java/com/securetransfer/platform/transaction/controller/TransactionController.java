package com.securetransfer.platform.transaction.controller;

import com.securetransfer.platform.transaction.dto.TransactionRequest;
import com.securetransfer.platform.transaction.dto.TransactionResponse;
import com.securetransfer.platform.transaction.dto.WithdrawalRequest;
import com.securetransfer.platform.transaction.entity.Transaction;
import com.securetransfer.platform.transaction.service.TransactionService;
import com.securetransfer.platform.transaction.service.WithdrawalService;
import com.securetransfer.platform.user.dto.UserResponse;
import com.securetransfer.platform.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final WithdrawalService withdrawalService;
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(@RequestBody TransactionRequest request,
            Authentication authentication) {
        UserResponse sender = userService.getUserByEmail(authentication.getName());

        Transaction transaction = transactionService.processTransfer(
                sender.id(),
                request.receiverId(),
                request.amount(),
                request.idempotencyKey(),
                request.type());

        return mapToResponse(transaction);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@transactionSecurity.isOwner(#id, authentication.name)")
    public TransactionResponse getTransaction(@PathVariable Long id) {
        Transaction transaction = transactionService.getTransaction(id);
        return mapToResponse(transaction);
    }

    @PostMapping("/{id}/withdraw")
    public TransactionResponse processWithdrawal(@PathVariable Long id, @RequestBody WithdrawalRequest request) {
        Transaction transaction = withdrawalService.processWithdrawal(id, request.code());
        return mapToResponse(transaction);
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getSenderId(),
                t.getReceiverId(),
                t.getAmount(),
                t.getFee(),
                t.getStatus(),
                t.getType(),
                t.getWithdrawalCode(),
                t.getWithdrawalExpiresAt(),
                t.getCreatedAt());
    }
}
