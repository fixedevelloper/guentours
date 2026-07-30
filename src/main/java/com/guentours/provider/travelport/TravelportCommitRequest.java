package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Body of Travelport's Workbench Commit
 * ({@code POST /air/book/reservation/reservations/{workbenchId}}), matching a real production
 * reference client. Committing with {@code payLaterInd=true} (no {@code Issuance} query param)
 * books and creates the PNR; committing again on the same locator with {@code Issuance=Ticket} and
 * {@code payLaterInd=false} (once payment has been collected) issues the tickets. The query
 * parameters live alongside this body in {@link TravelportClient#commit}.
 *
 * <p>Like {@link TravelportAddOfferRequest}, the body is wrapped under a top-level field named
 * after its own {@code @type} ({@code ReservationQueryCommitReservation}) - a flat, unwrapped body
 * (this record's own earlier shape) is rejected outright by Travelport's gateway with a bare HTTP
 * 400 before the request ever reaches its booking logic.
 *
 * <p>{@code ReceivedFrom} is this agency's signature on the booking (max 11 chars per the
 * reference's own comment, though its own value is longer - kept verbatim since it's presumably
 * what the account expects). The other booleans/enum all come from the reference's fixed defaults:
 * accept minor schedule changes and re-prices rather than fail the commit outright, but still fail
 * if the offer's price itself changed from what was quoted to the customer. {@code Notification} is
 * an optional array Travelport's own sample shows but never populates with real data for us
 * (a templated {@code notificationDate} placeholder) - omitted here.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TravelportCommitRequest(ReservationQueryCommitReservation ReservationQueryCommitReservation) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"@type", "scheduleChangeAcceptedInd", "errorWhenOfferPriceCancelledInd",
            "inhibitResidualDocumentIssuanceInd", "enableTwoStepCommitInd", "overrideMCTInd",
            "errorWhenScheduleChangesInd", "scheduleChangeReprice", "ReceivedFrom",
            "errorWhenOfferPriceChangesInd"})
    record ReservationQueryCommitReservation(
            @JsonProperty("@type") String type,
            Boolean scheduleChangeAcceptedInd,
            Boolean errorWhenOfferPriceCancelledInd,
            Boolean inhibitResidualDocumentIssuanceInd,
            Boolean enableTwoStepCommitInd,
            Boolean overrideMCTInd,
            Boolean errorWhenScheduleChangesInd,
            String scheduleChangeReprice,
            String ReceivedFrom,
            Boolean errorWhenOfferPriceChangesInd
    ) {
    }
}
