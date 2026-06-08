// ResourceNotFoundException.java — Ressource introuvable (ID inexistant)
package com.securetransfer.platform.common.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
