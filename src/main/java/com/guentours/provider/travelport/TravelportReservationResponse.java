package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response envelope of Travelport's Workbench Commit
 * ({@code POST /air/book/reservation/reservations/{workbenchId}}). Committing a workbench with no
 * payment books the itinerary and creates the PNR; committing with payment (and
 * {@code ?Issuance=Ticket}) issues the ticket(s).
 *
 * <p>Real production testing shows Travelport does not commit to one single response shape for
 * this call - a verified reference client falls back through three different candidate paths for
 * the record locator ({@code Reservation.locatorCode}, {@code ReservationResponse.Reservation.
 * locatorCode}, {@code ReservationDisplayResponse.ReservationShort.Identifier.value}) - on top of
 * the flat {@code ReservationResponse.Identifier.value} shape a verified mock sample used. All four
 * are modelled here as optional fields; {@link TravelportClient#confirmationFrom} tries them in
 * order and uses whichever one is actually populated.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TravelportReservationResponse(
        Reservation Reservation,
        ReservationResponse ReservationResponse,
        ReservationDisplayResponse ReservationDisplayResponse
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ReservationResponse(
            String transactionId,
            String reservationStatus,
            TravelportSearchResponse.Result Result,
            Identifier Identifier,
            Reservation Reservation
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Reservation(String locatorCode, Identifier Identifier) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ReservationDisplayResponse(ReservationShort ReservationShort) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ReservationShort(Identifier Identifier) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Identifier(String value, String authority) {
    }
}
