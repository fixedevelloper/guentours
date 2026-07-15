package com.guentours.provider.travelport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the shape our DTOs expect for Travelport's JSON air-shopping endpoint
 * ({@code POST /air/catalog/search/catalogproductofferings}) - aligned with a verified real
 * request/response sample. Key facts pinned: the request's top-level {@code @type} discriminator
 * (not a wrapper field), and the response resolving flights via {@code ProductBrandOptions.flightRefs}
 * against a {@code ReferenceListFlight}.
 */
class TravelportSearchResponseTest {

    private static final String SAMPLE_RESPONSE = """
            {
                "CatalogProductOfferingsResponse": {
                    "@type": "response",
                    "transactionId": "49f58f5f-c443-43b4-9f5d-be405fd00a01",
                    "CatalogProductOfferings": {
                        "@type": "CatalogProductOfferings",
                        "id": "offerings_1",
                        "Identifier": { "value": "A0656EFF-FAF4-456F-B061-0161008D7C4E", "authority": "TVPT" },
                        "CatalogProductOffering": [
                            {
                                "@type": "CatalogProductOffering",
                                "id": "cpo_1",
                                "Identifier": { "value": "OFFERING-ID-1", "authority": "TVPT" },
                                "sequence": 1,
                                "Departure": "DEN",
                                "Arrival": "ORD",
                                "ProductBrandOptions": [
                                    {
                                        "@type": "ProductBrandOptions",
                                        "flightRefs": [ "s1", "s2" ],
                                        "ProductBrandOffering": [
                                            {
                                                "Price": { "TotalPrice": 245.6, "CurrencyCode": "USD" }
                                            }
                                        ]
                                    }
                                ]
                            }
                        ]
                    },
                    "ReferenceList": [
                        {
                            "@type": "ReferenceListFlight",
                            "Flight": [
                                {
                                    "id": "s1",
                                    "carrier": "UA",
                                    "number": "123",
                                    "classOfService": "Y",
                                    "Departure": { "location": "DEN", "date": "2026-04-20", "time": "08:00:00" },
                                    "Arrival": { "location": "DFW", "date": "2026-04-20", "time": "10:30:00" }
                                },
                                {
                                    "id": "s2",
                                    "carrier": "UA",
                                    "number": "456",
                                    "classOfService": "Y",
                                    "Departure": { "location": "DFW", "date": "2026-04-20", "time": "11:30:00" },
                                    "Arrival": { "location": "ORD", "date": "2026-04-20", "time": "14:00:00" }
                                }
                            ]
                        }
                    ],
                    "Result": {
                        "@type": "Result",
                        "status": "Complete"
                    }
                }
            }
            """;

    @Test
    void deserializesTheFlightRefWireShape() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        TravelportSearchResponse response = mapper.readValue(SAMPLE_RESPONSE, TravelportSearchResponse.class);

        var body = response.CatalogProductOfferingsResponse();
        assertThat(body).isNotNull();
        assertThat(body.transactionId()).isEqualTo("49f58f5f-c443-43b4-9f5d-be405fd00a01");
        assertThat(body.Result().status()).isEqualTo("Complete");

        assertThat(body.CatalogProductOfferings().id()).isEqualTo("offerings_1");
        assertThat(body.CatalogProductOfferings().Identifier().value()).isEqualTo("A0656EFF-FAF4-456F-B061-0161008D7C4E");

        var offering = body.CatalogProductOfferings().CatalogProductOffering().get(0);
        assertThat(offering.id()).isEqualTo("cpo_1");
        assertThat(offering.Identifier().value()).isEqualTo("OFFERING-ID-1");
        assertThat(offering.Departure()).isEqualTo("DEN");

        var brandOption = offering.ProductBrandOptions().get(0);
        assertThat(brandOption.flightRefs()).containsExactly("s1", "s2");
        assertThat(brandOption.ProductBrandOffering().get(0).Price().TotalPrice()).isEqualTo(245.6);
        assertThat(brandOption.ProductBrandOffering().get(0).Price().CurrencyCode()).isEqualTo("USD");

        var referenceList = body.ReferenceList().get(0);
        assertThat(referenceList.type()).isEqualTo("ReferenceListFlight");
        var firstFlight = referenceList.Flight().get(0);
        assertThat(firstFlight.id()).isEqualTo("s1");
        assertThat(firstFlight.carrier()).isEqualTo("UA");
        assertThat(firstFlight.number()).isEqualTo("123");
        assertThat(firstFlight.Departure().location()).isEqualTo("DEN");
        assertThat(firstFlight.Departure().time()).isEqualTo("08:00:00");
        assertThat(referenceList.Flight().get(1).Arrival().location()).isEqualTo("ORD");
    }

    @Test
    void serializesARequestWithTopLevelTypeDiscriminator() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var request = new TravelportSearchRequest(
                "CatalogProductOfferingsQueryRequest",
                new TravelportSearchRequest.CatalogProductOfferingsRequest(
                        "CatalogProductOfferingsRequestAir",
                        1,
                        15,
                        List.of("GDS"),
                        List.of(new TravelportSearchRequest.PassengerCriteria("PassengerCriteria", 1, null, "ADT")),
                        List.of(new TravelportSearchRequest.SearchCriteriaFlight(
                                "SearchCriteriaFlight",
                                "2026-04-20",
                                new TravelportSearchRequest.Endpoint("DEN"),
                                new TravelportSearchRequest.Endpoint("ORD"))),
                        null));

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"@type\":\"CatalogProductOfferingsQueryRequest\"");
        assertThat(json).contains("\"@type\":\"CatalogProductOfferingsRequestAir\"");
        assertThat(json).doesNotContain("CatalogProductOfferingsQueryRequest\":{");
        assertThat(json).contains("\"offersPerPage\":15");
        assertThat(json).contains("\"contentSourceList\":[\"GDS\"]");
        assertThat(json).contains("\"passengerTypeCode\":\"ADT\"");
        assertThat(json).contains("\"departureDate\":\"2026-04-20\"");
        assertThat(json).contains("\"From\":{\"value\":\"DEN\"}");
        assertThat(json).doesNotContain("\"age\"");
    }
}
