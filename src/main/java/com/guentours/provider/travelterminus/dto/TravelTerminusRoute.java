package com.guentours.provider.travelterminus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * One itinerary option from Search or Revalidate's {@code route}/{@code route[]}. {@code flightObject}
 * is an opaque, vendor-encrypted token: never read or modify its fields, only pass it through
 * verbatim to the next call in the flow (Branded Fare, then Revalidate, then Book).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TravelTerminusRoute(
        Boolean isRefundable,
        Boolean isLcc,
        String airlineType,
        List<String> totalDuration,
        List<String> totalInterval,
        List<List<TravelTerminusSegment>> flightSegments,
        List<TravelTerminusFare> fare,
        JsonNode flightObject
) {
}
