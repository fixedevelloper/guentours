/**
 * Search module: fans a flight/hotel search out to every enabled
 * {@code TravelProviderClient} in parallel and harmonizes the raw offers -
 * merging quotes for the same product across providers and keeping the
 * lowest price - into a single response for the client.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"provider", "shared"}
)
package com.guentours.search;
