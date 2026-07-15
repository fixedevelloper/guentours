package com.guentours.provider.sabre;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response body of Sabre's Booking Management API {@code POST /v1/trip/orders/createBooking},
 * hotel variant. As with the flight side ({@link SabreCreateBookingResponse}), a successful
 * call returns the Sabre confirmation id (the order/PNR locator) for the whole trip; the
 * per-hotel {@code itemId} is what a later itemized cancellation would address, mirroring the
 * confirmed {@code cancelBooking} contract.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record SabreHotelBookingResponse(String confirmationId, List<Hotel> hotels, List<BookingError> errors) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Hotel(int itemId, String confirmationId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BookingError(String category, String type, String description) {
    }
}
