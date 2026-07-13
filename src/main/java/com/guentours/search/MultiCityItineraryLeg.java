package com.guentours.search;

import java.time.LocalDateTime;

/** One flight segment of a combined MULTI_CITY itinerary, priced by a single provider. */
public record MultiCityItineraryLeg(
        int legIndex,
        String airline,
        String flightNumber,
        String origin,
        String destination,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        String cabinClass,
        /** Offer id to pass back (one per leg, in order) when booking this itinerary. */
        String offerId
) {
}
