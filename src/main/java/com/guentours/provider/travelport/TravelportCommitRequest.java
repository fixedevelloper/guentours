package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body of Travelport's Workbench Commit
 * ({@code POST /air/book/reservation/reservations/{workbenchId}}), matching a real captured
 * Travelport DevKit Postman trace (request and its successful response - PNR, receipts, travelers,
 * full itinerary - verified together). Committing with {@code payLaterInd=true} (no
 * {@code Issuance} query param) books and creates the PNR; committing again on the same locator
 * with {@code Issuance=Ticket} and {@code payLaterInd=false} (once payment has been collected)
 * issues the tickets. The query parameters live alongside this body in
 * {@link TravelportClient#commit}.
 *
 * <p>Like {@link TravelportAddOfferRequest}, the body is wrapped under a top-level field named
 * after its own {@code @type} ({@code ReservationQueryCommitReservation}) - a flat, unwrapped body
 * is rejected outright by Travelport's gateway with a bare HTTP 400 before the request ever reaches
 * its booking logic. The verified trace carries nothing but that {@code @type} (plus an optional
 * {@code Notification} array we have no real data for and so omit) - an earlier, unverified attempt
 * added a set of schedule-change/pricing booleans and a {@code ReceivedFrom} agency signature that
 * don't appear here at all.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TravelportCommitRequest(ReservationQueryCommitReservation ReservationQueryCommitReservation) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ReservationQueryCommitReservation(@JsonProperty("@type") String type) {
    }
}
