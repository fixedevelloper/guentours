package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response of Travelport's document-production (ticketing) step
 * ({@code POST /book/documentproduction/tickets}), issuing e-tickets for a booked reservation
 * after payment. Verify field names against a live sandbox response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TravelportTicketResponse(List<Document> Document) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Document(String ticketNumber, String status) {
    }
}
