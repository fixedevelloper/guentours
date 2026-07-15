package com.guentours.provider;

import com.guentours.shared.Money;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Canonical flight offer shape every provider adapter must map its vendor-specific
 * response into, so the search module can compare/merge offers across providers.
 *
 * <p>{@code providerContext} is an opaque, provider-private bag of extra identifiers an adapter
 * captured at search time and needs back at booking time (e.g. Travelport's CatalogProductOfferings
 * container id for the Add Offer reference payload). It is not part of the canonical shape the
 * search module reasons about - only the originating adapter reads its own keys - and it is
 * excluded from {@link #harmonizationKey()} so it never affects cross-provider merging.
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
        int seatsAvailable,
        Map<String, String> providerContext
) {

    /** Convenience constructor for offers without any provider-specific booking context. */
    public FlightOffer(ProviderType providerType, String providerOfferId, String airline, String flightNumber,
                       String origin, String destination, LocalDateTime departureTime, LocalDateTime arrivalTime,
                       String cabinClass, Money price, int seatsAvailable) {
        this(providerType, providerOfferId, airline, flightNumber, origin, destination, departureTime, arrivalTime,
                cabinClass, price, seatsAvailable, Map.of());
    }

    public FlightOffer {
        providerContext = providerContext == null ? Map.of() : Map.copyOf(providerContext);
    }

    /** Key used to detect that two providers are quoting the same physical flight. */
    public String harmonizationKey() {
        return "%s|%s|%s|%s|%s".formatted(airline, flightNumber, origin, destination, departureTime.toLocalDate());
    }

    /** Reads a provider-specific context value captured at search time, or {@code null} if absent. */
    public String context(String key) {
        return providerContext.get(key);
    }
}
