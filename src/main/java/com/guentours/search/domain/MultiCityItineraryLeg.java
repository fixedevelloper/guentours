package com.guentours.search.domain;

import com.guentours.provider.FlightOfferDetail;

import java.time.LocalDateTime;

/** One flight segment of a combined MULTI_CITY itinerary, priced by a single provider. */
public record MultiCityItineraryLeg(
        int legIndex,
        String airline,
        String airlineName,
        String flightNumber,
        String origin,
        String destination,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        String cabinClass,
        /** Offer id to pass back (one per leg, in order) when booking this itinerary. */
        String offerId,
        /** Stops/baggage/hold detail of the underlying offer - null for providers that don't
         *  surface it (see {@link com.guentours.provider.FlightOffer#detail()}). */
        FlightOfferDetail detail
) {
}
