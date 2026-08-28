package com.guentours.provider.travelterminus.dto;

/** Body for {@code POST /api/flights/cancel-request}. Only {@code requestType=1}
 *  (FullCancellation) and {@code cancellationType=2} (FlightCancelled) are currently accepted by
 *  the API - every other enum value documented is reserved/not yet available. */
public record TravelTerminusCancelRequest(
        String bookingRefId,
        int requestType,
        int cancellationType,
        String cancelReason,
        String endUserIP
) {
    public static TravelTerminusCancelRequest fullCancellation(String bookingRefId, String reason, String endUserIP) {
        return new TravelTerminusCancelRequest(bookingRefId, 1, 2, reason, endUserIP);
    }
}
