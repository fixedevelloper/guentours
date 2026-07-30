/**
 * Search module: fans a flight/hotel search out to every enabled
 * {@code TravelProviderClient} in parallel and harmonizes the raw offers -
 * merging quotes for the same product across providers and keeping the
 * lowest price - into a single response for the client.
 *
 * <p>Depends on {@code geo} to resolve a searched hotel city's coordinates, since the city
 * autocomplete a traveler picks from carries no IATA-style code - only a name - and some
 * providers (e.g. Travelport) need lat/lon to search reliably.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"provider", "shared", "geo"}
)
package com.guentours.search;
