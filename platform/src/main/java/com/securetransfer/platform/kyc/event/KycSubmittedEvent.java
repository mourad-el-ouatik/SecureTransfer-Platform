package com.securetransfer.platform.kyc.event;

import com.securetransfer.platform.kyc.entity.KycDocument;
import org.springframework.context.ApplicationEvent;

public class KycSubmittedEvent extends ApplicationEvent {

    private final KycDocument kycDocument;
    private final String userEmail;

    public KycSubmittedEvent(Object source, KycDocument kycDocument, String userEmail) {
        super(source);
        this.kycDocument = kycDocument;
        this.userEmail = userEmail;
    }

    public KycDocument getKycDocument() { return kycDocument; }
    public String getUserEmail() { return userEmail; }
}