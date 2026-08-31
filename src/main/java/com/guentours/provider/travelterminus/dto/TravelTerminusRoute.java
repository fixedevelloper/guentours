package com.guentours.provider.travelterminus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * One itinerary option from Search or Revalidate's {@code route}/{@code route[]}. {@code flightObject}
 * is an opaque, vendor-encrypted token: never read or modify its fields, only pass it through
 * verbatim to the next call in the flow (Branded Fare, then Revalidate, then Book).
 *
 * <p>{@code isRefundable} is typed {@link JsonNode} rather than {@code Boolean}: the real Stage
 * sandbox returns a plain boolean in Search's {@code route[]} but an array ({@code [true]}) in
 * Revalidate's {@code route} - a single {@code Boolean} field crashes deserialization of whichever
 * shape it doesn't match. Not currently read by any business logic, so this stays permissive
 * rather than guessing which shape is "correct".
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TravelTerminusRoute(
        JsonNode isRefundable,
        Boolean isHoldAvailable,
        Boolean isLcc,
        String airlineType,
        List<String> totalDuration,
        List<String> totalInterval,
        List<List<TravelTerminusSegment>> flightSegments,
        List<TravelTerminusFare> fare,
        JsonNode flightObject
) {
}
