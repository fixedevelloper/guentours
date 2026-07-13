package com.guentours.shared.exception;

/** Raised when a request is well-formed but violates a business rule (e.g. paying an already-paid booking). */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
