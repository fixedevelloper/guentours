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
                        "@type": "Properties",
                        "Identifier": { "value": "d84ecc13-2ebf-4e33-8ecc-132ebf8e33b0" },
                        "totalProperties": 1,
                        "propertiesPerPage": 100,
                        "numberOfPages": 1,
                        "PropertyInfo": [
                            {
                                "@type": "PropertyInfo",
                                "id": "prop_1",
                                "Identifier": { "authority": "TVPT" },
                                "availability": "Open",
                                "Distance": { "value": 0.07, "unitOfDistance": "Miles" },
                                "Property": {
                                    "id": "prop_1",
                                    "name": "Grand Central Hotel",
                                    "PropertyKey": { "chainCode": "UR", "propertyCode": "G3375" },
                                    "Rating": [ { "value": 5, "provider": "NTM" } ],
                                    "Address": {
                                        "City": "Dublin",
                                        "StateProv": { "value": "CA", "name": "California" },
                                        "Country": { "value": "US", "name": "United States" }
                                    },
                                    "Image": [
                                        {
                                            "value": "https://media.iceportal.com/124930/photos/74156923_M.jpg",
                                            "dimensionCategory": "M",
                                            "caption": "On-Site,Outdoor/Exterior,Exterior View of Building",
                                            "pictureCategory": 1
                                        }
                                    ]
                                },
                                "LowestAvailableRate": { "value": 124.56, "code": "USD" },
                                "MaximumAvailableRate": { "value": 210.00, "code": "USD" }
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
        assertThat(properties.propertiesPerPage()).isEqualTo(100);
        assertThat(properties.numberOfPages()).isEqualTo(1);
        var info = properties.PropertyInfo().get(0);
        assertThat(info.id()).isEqualTo("prop_1");
        assertThat(info.Identifier().authority()).isEqualTo("TVPT");
        assertThat(info.availability()).isEqualTo("Open");
        assertThat(info.Distance().value()).isEqualTo(0.07);
        assertThat(info.Property().name()).isEqualTo("Grand Central Hotel");
        assertThat(info.Property().PropertyKey().chainCode()).isEqualTo("UR");
        assertThat(info.Property().Rating().get(0).value()).isEqualTo(5.0);
        assertThat(info.Property().Address().City()).isEqualTo("Dublin");
        assertThat(info.Property().Image().get(0).value())
                .isEqualTo("https://media.iceportal.com/124930/photos/74156923_M.jpg");
        assertThat(info.LowestAvailableRate().value()).isEqualTo(124.56);
        assertThat(info.LowestAvailableRate().code()).isEqualTo("USD");
        assertThat(info.MaximumAvailableRate().value()).isEqualTo(210.00);
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
                        "RoomStayCandidate",
                        new TravelportHotelSearchRequest.GuestCounts("GuestCounts", List.of(
                                new TravelportHotelSearchRequest.GuestCount("GuestCount", 2, "10"))))),
                new TravelportHotelSearchRequest.SearchBy("SearchByCity",
                        new TravelportHotelSearchRequest.SearchRadius(25, "Kilometers"), "PAR"),
                true));

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"@type\":\"PropertiesQuerySearch\"");
        assertThat(json).contains("\"CheckInDate\":\"2026-09-11\"");
        assertThat(json).contains("\"@type\":\"SearchByCity\"");
        assertThat(json).contains("\"SearchCity\":\"PAR\"");
        assertThat(json).contains("\"@type\":\"RoomStayCandidate\"");
        assertThat(json).contains("\"count\":2");
        assertThat(json).contains("\"returnOnlyAvailablePropertiesInd\":true");
    }
}
