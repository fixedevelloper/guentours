package com.guentours.provider.travelterminus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * {@code data} of the Branded Fare response. {@code brandedFares} follows one of three shapes -
 * see {@link TravelTerminusFlightObjectRef}: empty (no branded tiers, fall back to the original
 * fare), one group (round-trip priced as a single unit), or two groups (outbound/inbound priced
 * separately). Deeper per-tier merchandising detail (baggage/services/otherBenefits) is
 * display-only and intentionally not modeled here - only {@code flightObject} (needed for
 * Revalidate/Book) and {@code totalFare} (needed to pick the cheapest/base tier) are read.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TravelTerminusBrandedFareResponse(
        String status,
        String searchReqId,
        String hashReqKey,
        JsonNode searchObject,
        List<Group> brandedFares
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Group(String departure, String arrival, List<Tier> fare) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Tier(String fareType, Double totalFare, JsonNode flightObject) {
    }
}
