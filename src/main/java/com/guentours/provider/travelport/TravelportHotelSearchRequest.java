package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request body for Travelport's Stays "Search Properties by Location" endpoint
 * ({@code POST /hotel/search/properties/search}), matching a verified real sample. Searches for
 * properties by geo-coordinates, address, IATA airport code or IATA city code; the {@code SearchBy}
 * discriminator selects which. GuenTours searches by IATA city code, so {@code @type} is
 * {@code SearchByCityCode}.
 *
 * <p>The verified sample used {@code SearchByAirport} and did not show the location value field, so
 * the exact field name carrying the city code is a best-effort ({@code cityCode}); verify against a
 * real city-code search. Rooms/rates detail is not returned by this search - it comes from a
 * follow-up availability step.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TravelportHotelSearchRequest(PropertiesQuerySearch PropertiesQuerySearch) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PropertiesQuerySearch(
            @JsonProperty("@type") String type,
            String CheckInDate,
            String CheckOutDate,
            String RequestedCurrency,
            List<RoomStayCandidate> RoomStayCandidate,
            SearchBy SearchBy,
            Boolean returnOnlyAvailablePropertiesInd
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record RoomStayCandidate(GuestCounts GuestCounts) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GuestCounts(
            @JsonProperty("@type") String type,
            List<GuestCount> GuestCount
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GuestCount(
            @JsonProperty("@type") String type,
            int count,
            String ageQualifyingCode
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record SearchBy(
            @JsonProperty("@type") String type,
            String cityCode,
            SearchRadius SearchRadius
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record SearchRadius(int value, String unitOfDistance) {
    }
}
