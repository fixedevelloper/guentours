package com.guentours.provider.travelterminus.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Body for {@code POST /api/flights/pre-ancillary} - the optional step between Revalidate and
 * Book that prices baggage/meal/seat extras. {@code searchReqId}/{@code hashReqKey}/{@code
 * flightObject} must be the ones returned by Revalidate, not the original Search/Branded Fare
 * ones. {@code passengers} is only strictly required when Revalidate's {@code
 * bookingRequiredValidation.seatAncillaryRequiresPassengers} is {@code true} (see
 * TravelTerminusRevalidateResponse) - basic demographics only, no document fields.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TravelTerminusPreAncillaryRequest(
        String searchReqId,
        String hashReqKey,
        JsonNode flightObject,
        String endUserIP,
        String endUserBrowserAgent,
        List<Passenger> passengers
) {
    public record Passenger(String passengerType, String title, String firstName, String lastName) {
    }
}
