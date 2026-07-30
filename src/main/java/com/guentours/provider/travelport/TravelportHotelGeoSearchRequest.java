package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Geo-coordinate variant of Travelport's Stays "Search Properties by Location" request
 * ({@code POST /hotel/search/properties/search}), matching a verified real captured trace whose
 * {@code SearchBy} discriminator is {@code SearchByGeoLocation} (carrying {@code Latitude}/
 * {@code Longitude}) rather than {@link TravelportHotelSearchRequest}'s {@code SearchByCity}.
 *
 * <p>The city autocomplete a traveler picks from carries no IATA-style code, only a name, so
 * {@code TravelportClient} resolves the searched city's coordinates from our own hotel-city
 * reference data (see {@code HotelSearchCriteria#latitude()}/{@code longitude()}) and sends this
 * request instead; {@link TravelportHotelSearchRequest} is kept for a city with no matching
 * reference row to fall back on. The response shape is identical either way and is modelled once
 * in {@link TravelportHotelSearchResponse}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TravelportHotelGeoSearchRequest(PropertiesQuerySearch PropertiesQuerySearch) {

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
    record RoomStayCandidate(@JsonProperty("@type") String type, GuestCounts GuestCounts) {
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
            Integer age,
            int count,
            String ageQualifyingCode
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record SearchBy(
            @JsonProperty("@type") String type,
            SearchRadius SearchRadius,
            Double Latitude,
            Double Longitude
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record SearchRadius(double value, String unitOfDistance) {
    }
}
