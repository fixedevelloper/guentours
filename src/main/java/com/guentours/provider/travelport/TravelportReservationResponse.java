package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response envelope of Travelport's Workbench Commit
 * ({@code POST /air/book/reservation/reservations/{workbenchId}}). Committing a workbench with no
 * payment books the itinerary and creates the PNR; committing with payment (and
 * {@code ?Issuance=Ticket}) issues the ticket(s).
 *
 * <p>A real captured Travelport DevKit Postman trace shows a successful commit's shape:
 * {@code ReservationResponse.Reservation} carries the full itinerary/travelers plus a
 * {@code Receipt} list - one entry per confirmation the booking produced (an NDC order id, an
 * airline vendor locator, and a GDS-level locator with no {@code OfferRef}, i.e. not tied to one
 * specific offer). That last, offer-less receipt's {@code Confirmation.Locator.value} is the actual
 * GDS record locator (e.g. a 6-character {@code 1G} PNR) - the value this codebase needs to store
 * and later feed back into Post-Commit Workbench's {@code buildfromlocator?Locator=} for ticketing,
 * not the long internal {@code Reservation.Identifier.value} UUID (which real testing shows
 * Travelport itself never accepts back as a locator).
 *
 * <p>Real production testing also shows Travelport does not commit to one single response shape for
 * this call more generally - a verified reference client falls back through other candidate paths
 * for the record locator ({@code Reservation.locatorCode} at the top level,
 * {@code ReservationResponse.Reservation.locatorCode}, {@code ReservationDisplayResponse.
 * ReservationShort.Identifier.value}) on top of the flat {@code ReservationResponse.Identifier.value}
 * shape a verified mock sample used. All are modelled here as optional fields;
 * {@link TravelportClient#confirmationFrom} tries the Receipt-based GDS locator first, then falls
 * back through the rest in order.
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
    record Reservation(String locatorCode, Identifier Identifier, List<Receipt> Receipt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Receipt(Identifier Identifier, List<String> OfferRef, Confirmation Confirmation) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Confirmation(Locator Locator) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Locator(String source, String sourceContext, String value) {
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
