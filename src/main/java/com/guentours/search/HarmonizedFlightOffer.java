package com.guentours.search;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The result of merging every provider's quote for the same physical flight
 * (same airline, flight number, route and date) into one entry, ranked by price.
 */
public record HarmonizedFlightOffer(
        String airline,
        String flightNumber,
        String origin,
        String destination,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        String cabinClass,
        int seatsAvailable,
        String bestOfferId,
        List<ProviderQuote> quotes
) {
}
