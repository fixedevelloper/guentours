package com.guentours.provider.travelopro;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response from Travelopro's {@code /api/aeroVE5/ticket} endpoint. Best-effort inference
 * (not validated against a live sandbox response, unlike the Availability contract) -
 * confirm the real field names against Travelopro's Ticket API docs before going live.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TraveloproTicketResponse(TicketResult TicketResult) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TicketResult(String Status, String PNR, List<String> TicketNumbers, String Message, Error Error) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Error(String ErrorCode, String ErrorMessage) {
    }
}
