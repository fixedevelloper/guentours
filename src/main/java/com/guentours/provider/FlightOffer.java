package com.guentours.provider;

import com.guentours.shared.Money;

import java.time.LocalDateTime;

/**
 * Canonical flight offer shape every provider adapter must map its vendor-specific
 * response into, so the search module can compare/merge offers across providers.
 */
public record FlightOffer(
        ProviderType providerType,
        String providerOfferId,
        String airline,
        String flightNumber,
        String origin,
        String destination,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        String cabinClass,
        Money price,
        int seatsAvailable
) {
    /** Key used to detect that two providers are quoting the same physical flight. */
    public String harmonizationKey() {
        return "%s|%s|%s|%s|%s".formatted(airline, flightNumber, origin, destination, departureTime.toLocalDate());
    }
}
