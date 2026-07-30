package com.guentours.provider.travelport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the shape of Travelport's Stays "Search Properties by Location" request when searched by
 * coordinates instead of city name, against a verified real captured trace:
 * {@code SearchBy.@type} is {@code SearchByGeoLocation}, carrying {@code Latitude}/{@code Longitude}
 * rather than {@link TravelportHotelSearchRequest}'s {@code SearchByCity}/{@code SearchCity}.
 */
class TravelportHotelGeoSearchRequestTest {

    @Test
    void serializesAGeoLocationSearchRequest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var request = new TravelportHotelGeoSearchRequest(new TravelportHotelGeoSearchRequest.PropertiesQuerySearch(
                "PropertiesQuerySearch",
                "2026-09-11",
                "2026-09-15",
                "EUR",
                List.of(new TravelportHotelGeoSearchRequest.RoomStayCandidate(
                        "RoomStayCandidate",
                        new TravelportHotelGeoSearchRequest.GuestCounts("GuestCounts", List.of(
                                new TravelportHotelGeoSearchRequest.GuestCount("GuestCount", 54, 2, "10"))))),
                new TravelportHotelGeoSearchRequest.SearchBy("SearchByGeoLocation",
                        new TravelportHotelGeoSearchRequest.SearchRadius(38.5, "Kilometers"),
                        48.8589507, 2.2770198),
                true));

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"@type\":\"PropertiesQuerySearch\"");
        assertThat(json).contains("\"CheckInDate\":\"2026-09-11\"");
        assertThat(json).contains("\"@type\":\"RoomStayCandidate\"");
        assertThat(json).contains("\"age\":54");
        assertThat(json).contains("\"count\":2");
        assertThat(json).contains("\"@type\":\"SearchByGeoLocation\"");
        assertThat(json).contains("\"Latitude\":48.8589507");
        assertThat(json).contains("\"Longitude\":2.2770198");
        assertThat(json).doesNotContain("SearchCity");
        assertThat(json).contains("\"returnOnlyAvailablePropertiesInd\":true");
    }
}
