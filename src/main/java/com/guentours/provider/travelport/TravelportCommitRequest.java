package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Body of Travelport's Workbench Commit
 * ({@code POST /air/book/reservation/reservations/{workbenchId}}), matching a verified real sample.
 * Committing with no payment books and creates the PNR; committing with payment (plus
 * {@code ?Issuance=Ticket}) issues the tickets.
 *
 * <p>Only the fields GuenTours sets are modelled: {@code ReceivedFrom} (the agent/source of
 * record), {@code scheduleChangeAcceptedInd} (accept minor schedule changes rather than fail the
 * commit) and {@code errorWhenOfferPriceChangesInd} (fail if the price moved from what was quoted
 * to the customer, rather than silently book a different fare).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TravelportCommitRequest(
        String ReceivedFrom,
        Boolean scheduleChangeAcceptedInd,
        Boolean errorWhenOfferPriceChangesInd
) {
}
