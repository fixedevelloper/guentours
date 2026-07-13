package com.guentours.provider.travelopro;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response from Travelopro's {@code /api/aeroVE5/book} endpoint. Best-effort inference
 * (not validated against a live sandbox response, unlike the Availability contract) -
 * confirm the real field names against Travelopro's Book API docs before going live.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TraveloproBookResponse(BookResult BookResult) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BookResult(String Status, String BookingId, String PNR, String Message, Error Error) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Error(String ErrorCode, String ErrorMessage) {
    }
}
