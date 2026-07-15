package com.guentours.provider.travelport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the wire format of Travelport's Seat Map and build-ancillary-offers request/response
 * envelopes against verified real samples. NB: the sample seat-map response carried no seat
 * inventory grid (rows/seats/availability), so only the envelope + pricing is asserted here.
 */
class TravelportSeatAncillaryDtosTest {

    @Test
    void serializesASeatAvailabilityRequest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var request = new TravelportSeatAvailabilityRequest(
                "CatalogOfferingsQuerySeatAvailability",
                new TravelportSeatAvailabilityRequest.SeatAvailabilityOfferings(
                        "SeatAvailabilityOfferings",
                        List.of(new TravelportSeatAvailabilityRequest.CustomerLoyalty(
                                "132456", "Loyalty_1", "HY", "UA", "Silver", "John Smith"))));

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"@type\":\"CatalogOfferingsQuerySeatAvailability\"");
        assertThat(json).contains("\"@type\":\"SeatAvailabilityOfferings\"");
        assertThat(json).contains("\"programId\":\"HY\"");
        assertThat(json).contains("\"cardHolderName\":\"John Smith\"");
    }

    @Test
    void deserializesTheSeatMapResponseEnvelope() throws Exception {
        String sample = """
                {
                    "CatalogOfferingsAncillaryListResponse": {
                        "@type": "response",
                        "transactionId": "49f58f5f-c443-43b4-9f5d-be405fd00a01",
                        "CatalogOfferingsID": [
                            {
                                "@type": "CatalogOfferings",
                                "id": "CatalogOfferings_1",
                                "CatalogOffering": [
                                    {
                                        "@type": "CatalogOffering",
                                        "id": "108c5875",
                                        "ContentSource": "GDS",
                                        "Price": {
                                            "@type": "PriceDetail",
                                            "CurrencyCode": { "value": "USD" },
                                            "Base": 20.2,
                                            "TotalTaxes": 34.4,
                                            "TotalFees": 201,
                                            "TotalPrice": 34
                                        }
                                    }
                                ]
                            }
                        ],
                        "Result": { "@type": "Result", "status": "Complete" }
                    }
                }
                """;
        ObjectMapper mapper = new ObjectMapper();
        TravelportAncillaryListResponse response = mapper.readValue(sample, TravelportAncillaryListResponse.class);

        var body = response.CatalogOfferingsAncillaryListResponse();
        assertThat(body.transactionId()).isEqualTo("49f58f5f-c443-43b4-9f5d-be405fd00a01");
        assertThat(body.Result().status()).isEqualTo("Complete");
        var offering = body.CatalogOfferingsID().get(0).CatalogOffering().get(0);
        assertThat(offering.id()).isEqualTo("108c5875");
        assertThat(offering.Price().CurrencyCode().value()).isEqualTo("USD");
        assertThat(offering.Price().TotalPrice()).isEqualTo(34.0);
    }

    @Test
    void serializesAnAncillaryOfferRequest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var request = new TravelportAncillaryOfferRequest(
                "OfferQueryBuildAncillaryOffersFromCatalogOfferings",
                List.of(new TravelportAncillaryOfferRequest.BuildAncillaryOffersFromCatalogOfferings(
                        "BuildAncillaryOffersFromCatalogOfferings",
                        new TravelportAncillaryOfferRequest.Ref("CatalogOfferings_1",
                                new TravelportAncillaryOfferRequest.Identifier("A0656EFF", "TVPT")),
                        new TravelportAncillaryOfferRequest.Ref("co1",
                                new TravelportAncillaryOfferRequest.Identifier("A0656EFF", "TVPT")),
                        new TravelportAncillaryOfferRequest.Ref("product_1",
                                new TravelportAncillaryOfferRequest.Identifier("A0656EFF", "TVPT")),
                        3)));

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"@type\":\"OfferQueryBuildAncillaryOffersFromCatalogOfferings\"");
        assertThat(json).contains("\"CatalogOfferingsIdentifier\":{\"id\":\"CatalogOfferings_1\"");
        assertThat(json).contains("\"Quantity\":3");
    }

    @Test
    void deserializesTheOfferListResponse() throws Exception {
        String sample = """
                {
                    "OfferListResponse": {
                        "@type": "response",
                        "transactionId": "49f58f5f-c443-43b4-9f5d-be405fd00a01",
                        "OfferID": [
                            { "@type": "Offer", "id": "offer_1", "offerRef": "offer_1", "ContentSource": "GDS" }
                        ],
                        "Result": { "@type": "Result", "status": "Complete" }
                    }
                }
                """;
        ObjectMapper mapper = new ObjectMapper();
        TravelportOfferListResponse response = mapper.readValue(sample, TravelportOfferListResponse.class);

        var offer = response.OfferListResponse().OfferID().get(0);
        assertThat(offer.id()).isEqualTo("offer_1");
        assertThat(offer.offerRef()).isEqualTo("offer_1");
        assertThat(offer.ContentSource()).isEqualTo("GDS");
    }
}
