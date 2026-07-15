package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response envelope of Travelport's Workbench Commit
 * ({@code POST /air/book/reservation/reservations/{workbenchId}}), matching a verified real
 * sample. Committing a workbench with no payment books the itinerary and creates the PNR;
 * committing with payment (and {@code ?Issuance=Ticket}) issues the ticket(s).
 *
 * <p>{@code reservationStatus} / {@code Result.status} signal success, and {@code Identifier.value}
 * is the reservation identifier used here as the confirmation reference. The verified sample was a
 * mock that did not expose the classic airline record locator or issued ticket numbers - those are
 * retrieved via a separate Reservation Retrieve, so they are not modelled here.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TravelportReservationResponse(ReservationResponse ReservationResponse) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ReservationResponse(
            String transactionId,
            String reservationStatus,
            TravelportSearchResponse.Result Result,
            Identifier Identifier
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Identifier(String value, String authority) {
    }
}
