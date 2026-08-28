package com.guentours.provider.travelterminus.dto;

import com.fasterxml.jackson.databind.JsonNode;

/** Body for {@code POST /api/flights/branded-fare} - {@code flightObject} is the opaque object
 *  from the selected Search itinerary, passed through unmodified. Mandatory first step after
 *  Search: Revalidate cannot be called directly from a Search result. */
public record TravelTerminusBrandedFareRequest(
        String searchReqId,
        String hashReqKey,
        JsonNode flightObject,
        String endUserIP,
        String endUserBrowserAgent
) {
}
