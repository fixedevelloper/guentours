package com.guentours.provider.travelterminus.dto;

/** Body for {@code POST /api/flights/revalidate} (and, with the same shape, {@code /fare-rules}).
 *  {@code flightObject} is built from the Branded Fare response - see {@link TravelTerminusFlightObjectRef}. */
public record TravelTerminusRevalidateRequest(
        String searchReqId,
        String hashReqKey,
        TravelTerminusFlightObjectRef flightObject,
        String endUserIP,
        String endUserBrowserAgent
) {
}
