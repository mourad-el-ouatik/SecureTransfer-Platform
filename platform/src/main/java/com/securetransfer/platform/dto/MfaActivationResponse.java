package com.securetransfer.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MfaActivationResponse {
    private String secret;
    private String qrCodeBase64;  // Image en Base64 pour l'affichage
}