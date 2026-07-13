package com.guentours.provider.travelopro;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Request body for Travelopro's {@code /api/aeroVE5/availability} flight search endpoint. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TraveloproAvailabilityRequest(
        String user_id,
        String user_password,
        String access,
        String ip_address,
        String requiredCurrency,
        String journeyType,
        List<OriginDestinationInfoEntry> OriginDestinationInfo,
        @JsonProperty("class") String cabinClass,
        int adults,
        int childs,
        int infants
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record OriginDestinationInfoEntry(
            String departureDate,
            String returnDate,
            String airportOriginCode,
            String airportDestinationCode
    ) {
    }
}
