package com.guentours.provider;

import java.util.List;

/**
 * Rich, provider/fare-specific detail for one {@link FlightOffer}: its stop-by-stop itinerary,
 * per-segment baggage allowance, and whether the fare can still be held. Optional - a provider
 * adapter that doesn't surface this data leaves {@link FlightOffer#detail()} {@code null}.
 *
 * <p>Deliberately not part of {@link FlightOffer#harmonizationKey()}: two providers quoting the
 * same physical flight can still differ on hold availability or baggage allowance, so this stays
 * attached to each provider's own quote rather than the shared harmonized summary.
 */
public record FlightOfferDetail(
        Boolean holdAvailable,
        String totalDuration,
        String totalLayoverDuration,
        List<FlightSegmentDetail> segments
) {
}
