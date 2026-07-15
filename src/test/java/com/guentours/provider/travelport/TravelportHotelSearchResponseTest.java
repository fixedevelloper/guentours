package com.guentours.provider.travelport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the shape of Travelport's Stays "Search Properties by Location" request/response against a
 * verified real sample: properties come under {@code PropertiesResponse.Properties.PropertyInfo},
 * each with a {@code Property} (name/key/rating) and a {@code LowestAvailableRate}.
 */
class TravelportHotelSearchResponseTest {

    private static final String SAMPLE_RESPONSE = """
            {
                "PropertiesResponse": {
                    "Properties": {
                        "totalProperties": 1,
                        "PropertyInfo": [
                            {
                                "id": "prop_1",
                                "availability": "Open",
                                "Property": {
                                    "id": "prop_1",
                                    "name": "Grand Central Hotel",
                                    "PropertyKey": { "chainCode": "UR", "propertyCode": "G3375" },
                                    "Rating": [ { "value": 5, "provider": "NTM" } ],
                                    "Address": {
                                        "City": "Dublin",
                                        "StateProv": { "value": "CA", "name": "California" },
                                        "Country": { "value": "US", "name": "United States" }
                                    }
                                },
                                "LowestAvailableRate": { "value": 124.56, "code": "USD" }
                            }
                        ]
                    },
                    "@type": "response",
                    "reservationStatus": "Success"
                }
            }
            """;

    @Test
    void deserializesTheStaysSearchWireShape() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        TravelportHotelSearchResponse response = mapper.readValue(SAMPLE_RESPONSE, TravelportHotelSearchResponse.class);

        var properties = response.PropertiesResponse().Properties();
        assertThat(properties.totalProperties()).isEqualTo(1);
        var info = properties.PropertyInfo().get(0);
        assertThat(info.id()).isEqualTo("prop_1");
        assertThat(info.availability()).isEqualTo("Open");
        assertThat(info.Property().name()).isEqualTo("Grand Central Hotel");
        assertThat(info.Property().PropertyKey().chainCode()).isEqualTo("UR");
        assertThat(info.Property().Rating().get(0).value()).isEqualTo(5.0);
        assertThat(info.Property().Address().City()).isEqualTo("Dublin");
        assertThat(info.LowestAvailableRate().value()).isEqualTo(124.56);
        assertThat(info.LowestAvailableRate().code()).isEqualTo("USD");
    }

    @Test
    void serializesACityCodeSearchRequest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var request = new TravelportHotelSearchRequest(new TravelportHotelSearchRequest.PropertiesQuerySearch(
                "PropertiesQuerySearch",
                "2026-09-11",
                "2026-09-15",
                "EUR",
                List.of(new TravelportHotelSearchRequest.RoomStayCandidate(
                        new TravelportHotelSearchRequest.GuestCounts("GuestCounts", List.of(
                                new TravelportHotelSearchRequest.GuestCount("GuestCount", 2, "10"))))),
                new TravelportHotelSearchRequest.SearchBy("SearchByCityCode", "PAR",
                        new TravelportHotelSearchRequest.SearchRadius(25, "Kilometers")),
                true));

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"@type\":\"PropertiesQuerySearch\"");
        assertThat(json).contains("\"CheckInDate\":\"2026-09-11\"");
        assertThat(json).contains("\"@type\":\"SearchByCityCode\"");
        assertThat(json).contains("\"cityCode\":\"PAR\"");
        assertThat(json).contains("\"count\":2");
        assertThat(json).contains("\"returnOnlyAvailablePropertiesInd\":true");
    }
}
