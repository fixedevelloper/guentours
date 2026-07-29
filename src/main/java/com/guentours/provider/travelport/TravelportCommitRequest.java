package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Body of Travelport's Workbench Commit
 * ({@code POST /air/book/reservation/reservations/{workbenchId}}), matching a real production
 * reference client. Committing with {@code payLaterInd=true} (no {@code Issuance} query param)
 * books and creates the PNR; committing again on the same locator with {@code Issuance=Ticket} and
 * {@code payLaterInd=false} (once payment has been collected) issues the tickets. The query
 * parameters live alongside this body in {@link TravelportClient#commit}.
 *
 * <p>{@code ReceivedFrom} is this agency's signature on the booking (max 11 chars per the
 * reference's own comment, though its own value is longer - kept verbatim since it's presumably
 * what the account expects). The other booleans/enum all come from the reference's fixed defaults:
 * accept minor schedule changes and re-prices rather than fail the commit outright, but still fail
 * if the offer's price itself changed from what was quoted to the customer.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TravelportCommitRequest(
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
