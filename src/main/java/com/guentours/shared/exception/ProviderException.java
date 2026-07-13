package com.guentours.shared.exception;

/** Raised when a call to an upstream GDS/provider (Travelopro, Sabre, Travelport) fails or times out. */
public class ProviderException extends RuntimeException {

    public ProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProviderException(String message) {
        super(message);
    }
}
