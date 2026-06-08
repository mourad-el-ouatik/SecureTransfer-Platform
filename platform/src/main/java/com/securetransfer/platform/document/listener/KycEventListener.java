package com.securetransfer.platform.document.listener;

import com.securetransfer.platform.document.service.NotificationService;
import com.securetransfer.platform.kyc.event.KycReviewedEvent;
import com.securetransfer.platform.kyc.event.KycSubmittedEvent;
import com.securetransfer.platform.kyc.entity.KycDocumentState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KycEventListener {

    private final NotificationService notificationService;

    @EventListener
    @Async
    public void onKycSubmitted(KycSubmittedEvent event) {
        log.info("[ASYNC] KYC soumis pour {}", event.getUserEmail());
        try {
            notificationService.sendKycSubmitted(event.getUserEmail());
        } catch (Exception e) {
            log.error("Echec notif KYC submitted", e);
        }
    }

    @EventListener
    @Async
    public void onKycReviewed(KycReviewedEvent event) {
        log.info("[ASYNC] KYC revu pour {} — decision: {}", event.getUserEmail(), event.getDecision());
        try {
            if (event.getDecision() == KycDocumentState.VERIFIED) {
                notificationService.sendKycApproved(event.getUserEmail());
            } else {
                notificationService.sendKycRejected(event.getUserEmail());
            }
        } catch (Exception e) {
            log.error("Echec notif KYC reviewed", e);
        }
    }
}
