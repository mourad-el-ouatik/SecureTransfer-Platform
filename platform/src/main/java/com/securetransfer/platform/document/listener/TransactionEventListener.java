package com.securetransfer.platform.document.listener;

import com.securetransfer.platform.document.service.NotificationService;
import com.securetransfer.platform.document.service.PdfReceiptService;
import com.securetransfer.platform.transaction.entity.Transaction;
import com.securetransfer.platform.transaction.event.TransactionCreatedEvent;
import com.securetransfer.platform.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventListener {

    private final PdfReceiptService pdfReceiptService;
    private final NotificationService notificationService;
    private final UserService userService;

    @EventListener
    @Async
    public void onTransactionCreated(TransactionCreatedEvent event) {
        Transaction tx = event.getTransaction();
        log.info("[ASYNC] Post-traitement transaction #{}", tx.getId());

        // 1. Générer le PDF
        try {
            pdfReceiptService.generateReceipt(tx);
            log.info("[ASYNC] PDF genere pour transaction #{}", tx.getId());
        } catch (Exception e) {
            log.error("Echec generation PDF tx #{}", tx.getId(), e);
        }

        // 2. Envoyer email au sender authentifié
        try {
            String senderEmail = userService.getUser(tx.getSenderId()).email();
            notificationService.sendTransactionConfirmation(senderEmail, tx);
            log.info("[ASYNC] Email envoye a {} pour transaction #{}", senderEmail, tx.getId());
        } catch (Exception e) {
            log.error("Echec envoi email pour tx #{} : {}", tx.getId(), e.getMessage());
        }
    }
}
