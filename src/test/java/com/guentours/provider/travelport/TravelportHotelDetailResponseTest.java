package com.guentours.provider.travelport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the shape of Travelport's Stays "Get Property Details" response
 * ({@code GET /hotel/search/propertiesdetail}) against a verified real captured trace: the property
 * lives at {@code PropertiesResponse.Properties.PropertyInfo[0].Property}, with geo-coordinates,
 * photos, business/accessibility amenities and a room-count breakdown the property search response
 * never carries.
 */
class TravelportHotelDetailResponseTest {

    private static final String SAMPLE_RESPONSE = """
            {
                "PropertiesResponse": {
                    "Properties": {
                        "@type": "Properties",
                        "PropertyInfo": [
                            {
                                "@type": "PropertyInfo",
                                "id": "WI-B2095",
                                "Distance": { "value": 18.31, "unitOfDistance": "Miles" },
                                "Property": {
                                    "@type": "PropertyDetail",
                                    "id": "WI-B2095",
                                    "PropertyKey": { "@type": "PropertyKey", "chainCode": "WI", "propertyCode": "B2095" },
                                    "name": "The Westin Denver International Airport",
                                    "Rating": [ { "value": 4, "provider": "AAA" } ],
                                    "GeoLocation": { "latitude": 39.847611, "longitude": -104.674127 },
                                    "Image": [
                                        { "value": "https://example.com/exterior.jpg", "dimensionCategory": "L",
                                          "caption": "Exterior", "pictureCategory": 1 }
                                    ],
                                    "BusinessService": [ { "code": "10", "description": "Free toll free calls" } ],
                                    "AccessibilityFeature": [ { "value": "110", "description": "Roll-in shower available" } ],
                                    "GuestRoomInfo": [ { "@type": "GuestRoomInfo", "code": "12", "number": 484, "description": "Total rooms" } ],
                                    "Address": {
                                        "@type": "Address",
                                        "AddressLine": [ "8300 Pena Boulevard" ],
                                        "City": "Denver",
                                        "StateProv": { "value": "CO" },
                                        "Country": { "value": "US", "name": "United States" },
                                        "PostalCode": "80249     "
                                    },
                                    "Telephone": [ "303-317-1800", "317-1800" ],
                                    "Email": { "value": "westindenverairport@westin.com" },
                                    "PropertyAmenity": [
                                        { "@type": "PropertyAmenity", "description": "Housekeeping - daily", "code": "50", "category": "Housekeeping" }
                                    ]
                                }
                            }
                        ]
                    },
                    "traceId": "3835160943e3114574f0530841305f30"
                }
            }
            """;

    @Test
    void deserializesThePropertyDetailWireShape() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        TravelportHotelDetailResponse response = mapper.readValue(SAMPLE_RESPONSE, TravelportHotelDetailResponse.class);

        var propertyInfo = response.PropertiesResponse().Properties().PropertyInfo().get(0);
        assertThat(propertyInfo.id()).isEqualTo("WI-B2095");
        var property = propertyInfo.Property();
        assertThat(property.name()).isEqualTo("The Westin Denver International Airport");
        assertThat(property.PropertyKey().chainCode()).isEqualTo("WI");
        assertThat(property.PropertyKey().propertyCode()).isEqualTo("B2095");
        assertThat(property.GeoLocation().latitude()).isEqualTo(39.847611);
        assertThat(property.GeoLocation().longitude()).isEqualTo(-104.674127);
        assertThat(property.Image()).hasSize(1);
        assertThat(property.Image().get(0).value()).isEqualTo("https://example.com/exterior.jpg");
        assertThat(property.BusinessService().get(0).description()).isEqualTo("Free toll free calls");
        assertThat(property.AccessibilityFeature().get(0).description()).isEqualTo("Roll-in shower available");
        assertThat(property.GuestRoomInfo().get(0).number()).isEqualTo(484);
        assertThat(property.Address().City()).isEqualTo("Denver");
        assertThat(property.Address().Country().name()).isEqualTo("United States");
        assertThat(property.Telephone()).containsExactly("303-317-1800", "317-1800");
        assertThat(property.Email().value()).isEqualTo("westindenverairport@westin.com");
        assertThat(property.PropertyAmenity().get(0).description()).isEqualTo("Housekeeping - daily");
    }
}
