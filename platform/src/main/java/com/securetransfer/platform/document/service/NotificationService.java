package com.securetransfer.platform.document.service;

import com.securetransfer.platform.transaction.entity.Transaction;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendTransactionConfirmation(String toEmail, Transaction tx) {
        Context ctx = new Context();
        ctx.setVariable("transactionId", tx.getId());
        ctx.setVariable("amount", tx.getAmount());
        ctx.setVariable("fee", tx.getFee());
        ctx.setVariable("type", tx.getType().name());
        ctx.setVariable("status", tx.getStatus().name());
        ctx.setVariable("createdAt", tx.getCreatedAt());
        ctx.setVariable("withdrawalCode", tx.getWithdrawalCode());
        sendEmail(toEmail, "Confirmation de votre transaction #" + tx.getId(),
                "email/transaction-confirmation", ctx);
    }

    public void sendKycApproved(String toEmail) {
        Context ctx = new Context();
        ctx.setVariable("email", toEmail);
        sendEmail(toEmail, "Votre KYC a ete approuve — SecureTransfer",
                "email/kyc-approved", ctx);
    }

    public void sendKycRejected(String toEmail) {
        Context ctx = new Context();
        ctx.setVariable("email", toEmail);
        sendEmail(toEmail, "Votre KYC a ete rejete — SecureTransfer",
                "email/kyc-rejected", ctx);
    }

    public void sendKycSubmitted(String toEmail) {
        Context ctx = new Context();
        ctx.setVariable("email", toEmail);
        sendEmail(toEmail, "Votre dossier KYC a ete recu — SecureTransfer",
                "email/kyc-submitted", ctx);
    }

    private void sendEmail(String to, String subject, String template, Context ctx) {
        try {
            String body = templateEngine.process(template, ctx);
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(msg);
            log.info("Email envoye a {} — {}", to, subject);
        } catch (MessagingException e) {
            log.error("Echec envoi email a {} : {}", to, e.getMessage());
        } catch (Exception e) {
            log.error("Erreur inattendue envoi email a {} : {}", to, e.getMessage());
        }
    }
}
