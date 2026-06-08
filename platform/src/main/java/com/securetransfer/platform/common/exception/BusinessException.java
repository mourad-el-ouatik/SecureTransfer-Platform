// BusinessException.java — Erreur métier (email existant, limite dépassée...)
package com.securetransfer.platform.common.exception;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
