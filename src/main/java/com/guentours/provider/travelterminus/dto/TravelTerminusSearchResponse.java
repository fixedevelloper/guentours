package com.guentours.provider.travelterminus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * {@code data.searchResponse} of a {@code route_update} Streaming Search SSE event. Streaming
 * Search has no single top-level envelope like the other endpoints (see
 * {@link TravelTerminusEnvelope}) - each pushed event carries its own copy of this shape, and
 * {@code searchReqId}/{@code hashReqKey} are identical across every event of one session.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TravelTerminusSearchResponse(
        String status,
        String searchReqId,
        String hashReqKey,
        Meta meta,
        List<TravelTerminusRoute> route,
        String message
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(Integer totalResults, String currency, String airTripType, String cabinClass) {
    }
}
