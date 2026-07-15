package com.guentours.provider.travelport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the shape of Travelport's Stays Hotel Availability request/response against a verified real
 * sample: each {@code CatalogOffering} is a bookable rate whose {@code Price.TotalPrice} +
 * {@code CurrencyCode.value} give the fresh room rate.
 */
class TravelportHotelAvailabilityResponseTest {

    private static final String SAMPLE_RESPONSE = """
            {
                "CatalogOfferingsHospitalityResponse": {
                    "@type": "response",
                    "reservationStatus": "Success",
                    "CatalogOfferings": {
                        "@type": "CatalogOfferings",
                        "id": "CatalogOfferings_1",
                        "CatalogOffering": [
                            {
                                "@type": "CatalogOffering",
                                "id": "108c5875",
                                "CatalogOfferingRef": "co1",
                                "ContentSource": "GDS",
                                "Price": {
                                    "@type": "PriceDetail",
                                    "CurrencyCode": { "value": "USD" },
                                    "Base": 20.2,
                                    "TotalTaxes": 34.4,
                                    "TotalFees": 201,
                                    "TotalPrice": 255.6
                                }
                            }
                        ]
                    },
                    "Result": { "@type": "Result", "status": "Complete" }
                }
            }
            """;

    @Test
    void deserializesTheAvailabilityWireShape() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        TravelportHotelAvailabilityResponse response =
                mapper.readValue(SAMPLE_RESPONSE, TravelportHotelAvailabilityResponse.class);

        var body = response.CatalogOfferingsHospitalityResponse();
        assertThat(body.reservationStatus()).isEqualTo("Success");
        var offering = body.CatalogOfferings().CatalogOffering().get(0);
        assertThat(offering.id()).isEqualTo("108c5875");
        assertThat(offering.Price().TotalPrice()).isEqualTo(255.6);
        assertThat(offering.Price().CurrencyCode().value()).isEqualTo("USD");
    }

    @Test
    void serializesAnAvailabilityRequestByPropertyKey() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var request = new TravelportHotelAvailabilityRequest(
                new TravelportHotelAvailabilityRequest.CatalogOfferingsQueryRequest(
                        "CatalogOfferingsRequestHospitality",
                        List.of(new TravelportHotelAvailabilityRequest.CatalogOfferingsRequest(
                                "CatalogOfferingsRequestHospitality",
                                "EUR",
                                new TravelportHotelAvailabilityRequest.StayDates("2026-09-11", "2026-09-15"),
                                new TravelportHotelAvailabilityRequest.HotelSearchCriterion(
                                        "HotelSearchCriterion",
                                        1,
                                        List.of(new TravelportHotelAvailabilityRequest.PropertyRequest(
                                                "PropertyRequest",
                                                new TravelportHotelAvailabilityRequest.PropertyKey("UR", "G3375"))))))));

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"@type\":\"CatalogOfferingsRequestHospitality\"");
        assertThat(json).contains("\"chainCode\":\"UR\"");
        assertThat(json).contains("\"propertyCode\":\"G3375\"");
        assertThat(json).contains("\"start\":\"2026-09-11\"");
        assertThat(json).contains("\"numberOfRooms\":1");
    }
}
