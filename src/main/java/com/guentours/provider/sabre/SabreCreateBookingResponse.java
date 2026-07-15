package com.guentours.provider.sabre;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response body of Sabre's Booking Management API {@code POST /v1/trip/orders/createBooking}.
 * Per Sabre's official AirBookingWorkflow, a successful call returns the Sabre confirmation
 * id (the PNR record locator) plus the airline's own confirmation id(s) for each flight.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record SabreCreateBookingResponse(
        String confirmationId,
        List<Flight> flights,
        List<BookingError> errors
) {

    /** One booked flight; {@code confirmationId} is the AIRLINE's record locator for it. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Flight(String confirmationId, String flightNumber, String airlineCode) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BookingError(String category, String type, String description) {
    }
}
