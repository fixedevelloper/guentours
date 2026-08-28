package com.guentours.provider.travelterminus.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * The {@code flightObject} payload sent to Revalidate/Fare Rules/Book once a fare has been chosen
 * from Branded Fare: the opaque {@code searchObject} returned by Branded Fare, plus the selected
 * fare tier's {@code flightObject} for each leg. Per Travel Terminus's own "Preparing for
 * Revalidate & Fare Rule" guidance: 0 entries in {@code brandedFares} for a Standard/no-tier fare,
 * 1 for a combined round-trip (Full Journey), 2 for outbound+inbound priced separately (Split
 * Journey). Every field here is opaque and must be passed through unmodified.
 */
public record TravelTerminusFlightObjectRef(JsonNode searchObject, List<JsonNode> brandedFares) {
}
