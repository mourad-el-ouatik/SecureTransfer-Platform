package com.securetransfer.platform.user.entity;

public enum KycStatus {
    PENDING,    // En attente de vérification (valeur par défaut à la création)
    VERIFIED,   // Identité vérifiée par un admin
    REJECTED    // Documents refusés
}
