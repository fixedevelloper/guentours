package com.guentours.provider.travelterminus;

import com.guentours.shared.exception.ProviderException;

/** Raised for a Travel Terminus error response, carrying the vendor's machine-readable
 *  {@code code} (e.g. {@code insufficient_funds}, {@code token_expired}, {@code provider_down})
 *  alongside the human-readable message, so callers/logs can tell error categories apart without
 *  parsing the message text. */
public class TravelTerminusException extends ProviderException {

    private final String code;

    public TravelTerminusException(String code, String message) {
        super(message);
        this.code = code;
    }

    public TravelTerminusException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
