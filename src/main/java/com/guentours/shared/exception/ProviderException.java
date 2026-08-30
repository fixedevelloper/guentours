package com.guentours.shared.exception;

/** Raised when a call to an upstream GDS/provider (Travelopro, Sabre, Travelport) fails or times out. */
public class ProviderException extends RuntimeException {

    public ProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProviderException(String message) {
        super(message);
    }

    /** Machine-readable provider error code (e.g. Travel Terminus's {@code insufficient_funds}/
     *  {@code unauthorized}/{@code provider_down}/{@code invalid_request}), when the provider's
     *  response actually carried one - see {@link com.guentours.booking.domain.Booking
     *  #providerErrorCode}, which this feeds so an admin can tell error categories apart on the
     *  booking detail page without digging through logs. Null for providers/call sites that don't
     *  carry a machine-readable code (most Travelopro/Sabre/Travelport failures today). */
    public String code() {
        return null;
    }
}
