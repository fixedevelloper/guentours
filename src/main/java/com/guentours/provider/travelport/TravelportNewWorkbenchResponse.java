package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response envelope of New Workbench ({@code POST /air/book/session/reservationworkbench}),
 * matching a verified real production reference client. Travelport assigns the workbench id
 * server-side and returns it as {@code ReservationResponse.Reservation.Identifier.value}; every
 * later workbench-scoped call (Add Offer, Add Traveler, Commit, ...) must address
 * {@code .../reservationworkbench/{that id}/...} - a client-invented id is never recognized and
 * fails with "WORKBENCH ID IS NOT VALID".
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TravelportNewWorkbenchResponse(ReservationResponse ReservationResponse) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ReservationResponse(Reservation Reservation) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Reservation(Identifier Identifier) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Identifier(String value, String authority) {
    }
}
