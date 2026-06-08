package com.securetransfer.platform.kyc.event;

import com.securetransfer.platform.kyc.entity.KycDocument;
import com.securetransfer.platform.kyc.entity.KycDocumentState;
import org.springframework.context.ApplicationEvent;

public class KycReviewedEvent extends ApplicationEvent {

    private final KycDocument kycDocument;
    private final String userEmail;
    private final KycDocumentState decision;

    public KycReviewedEvent(Object source, KycDocument kycDocument, String userEmail, KycDocumentState decision) {
        super(source);
        this.kycDocument = kycDocument;
        this.userEmail = userEmail;
        this.decision = decision;
    }

    public KycDocument getKycDocument() { return kycDocument; }
    public String getUserEmail() { return userEmail; }
    public KycDocumentState getDecision() { return decision; }
}