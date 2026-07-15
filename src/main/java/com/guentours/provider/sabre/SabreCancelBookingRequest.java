package com.guentours.provider.sabre;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request body for Sabre's Booking Management API cancellation endpoint
 * ({@code POST /v1/trip/orders/cancelBooking}), the counterpart of
 * {@code /createBooking} - voids the whole reservation identified by the
 * Sabre confirmation id.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record SabreCancelBookingRequest(String confirmationId, boolean cancelAll) {
}
