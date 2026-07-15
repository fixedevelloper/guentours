package com.guentours.provider.travelport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the shape of Travelport's price, workbench-step and ticketing DTOs (the shop -> price ->
 * workbench book -> ticket flow), built from Travelport's documented JSON API conventions since
 * the per-step sandbox payloads were not reachable to verify against directly.
 */
class TravelportBookingDtosTest {

    @Test
    void serializesAPriceRequestByOfferingId() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var request = new TravelportPriceRequest(new TravelportPriceRequest.PriceProductsQueryRequest(
                "PriceProductsQueryRequest",
                List.of(new TravelportPriceRequest.CatalogProductOfferingSelection(
                        "CatalogProductOfferingSelection",
                        new TravelportPriceRequest.Identifier("off1")))));

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"@type\":\"PriceProductsQueryRequest\"");
        assertThat(json).contains("\"CatalogProductOfferingIdentifier\":{\"value\":\"off1\"}");
    }

    @Test
    void deserializesAPriceResponse() throws Exception {
        String sample = """
                {
                    "OffersResponse": {
                        "Offer": [
                            { "id": "O1", "Price": { "TotalPrice": 131.8, "CurrencyCode": "USD" } }
                        ]
                    }
                }
                """;
        ObjectMapper mapper = new ObjectMapper();
        TravelportPriceResponse response = mapper.readValue(sample, TravelportPriceResponse.class);

        var offer = response.OffersResponse().Offer().get(0);
        assertThat(offer.id()).isEqualTo("O1");
        assertThat(offer.Price().TotalPrice()).isEqualTo(131.8);
        assertThat(offer.Price().CurrencyCode()).isEqualTo("USD");
    }

    @Test
    void serializesANewWorkbenchReservationWithOfferAndTravelers() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var reservation = new TravelportWorkbenchRequests.Reservation(
                "Reservation",
                List.of(new TravelportWorkbenchRequests.Offer("Offer", "offer_1", "offer_1", null, "GDS")),
                List.of(new TravelportWorkbenchRequests.Traveler(
                        "Traveler", "1990-05-01", "ADT",
                        new TravelportWorkbenchRequests.PersonName("PersonNameDetail", null, "Jane", null, "Doe"),
                        List.of(new TravelportWorkbenchRequests.Email("jane.doe@example.com")))),
                null,
                null);

        String json = mapper.writeValueAsString(reservation);

        assertThat(json).contains("\"@type\":\"Reservation\"");
        assertThat(json).contains("\"id\":\"offer_1\"");
        assertThat(json).contains("\"@type\":\"Offer\"");
        assertThat(json).contains("\"@type\":\"PersonNameDetail\"");
        assertThat(json).contains("\"Given\":\"Jane\"");
        assertThat(json).contains("\"Surname\":\"Doe\"");
        assertThat(json).contains("\"passengerTypeCode\":\"ADT\"");
        assertThat(json).contains("\"Email\":[{\"value\":\"jane.doe@example.com\"}]");
    }

    @Test
    void serializesAnAddOfferReferencePayload() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var request = new TravelportAddOfferRequest(
                "OfferQueryBuildFromCatalogProductOfferings",
                new TravelportAddOfferRequest.BuildFromCatalogProductOfferingsRequest(
                        "BuildFromCatalogProductOfferingsRequestAir",
                        new TravelportAddOfferRequest.OfferingsRef("cpo_1", null),
                        List.of(new TravelportAddOfferRequest.CatalogProductOfferingSelection(
                                "CatalogProductOfferingSelection",
                                new TravelportAddOfferRequest.OfferingRef("cpo_1", null, "cpo_1"),
                                List.of(1, 2)))),
                4);

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"@type\":\"OfferQueryBuildFromCatalogProductOfferings\"");
        assertThat(json).contains("\"@type\":\"BuildFromCatalogProductOfferingsRequestAir\"");
        assertThat(json).contains("\"CatalogProductOfferingRef\":\"cpo_1\"");
        assertThat(json).contains("\"SegmentSequence\":[1,2]");
        assertThat(json).contains("\"MaxNumberOfUpsellsToReturn\":4");
    }

    @Test
    void deserializesAnAddOfferResponse() throws Exception {
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
        assertThat(offer.ContentSource()).isEqualTo("GDS");
    }

    @Test
    void serializesAnAddFormOfPaymentCashRequest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var fop = new TravelportFormOfPaymentRequest(
                "FormOfPaymentCash", true, true, null, "GuenTours txn TXN-1");

        String json = mapper.writeValueAsString(fop);

        assertThat(json).contains("\"@type\":\"FormOfPaymentCash\"");
        assertThat(json).contains("\"reservationFOPInd\":true");
        assertThat(json).contains("\"FreeText\":\"GuenTours txn TXN-1\"");
        assertThat(json).doesNotContain("\"Comment\"");
    }

    @Test
    void serializesAnAddPaymentRequest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var request = new TravelportPaymentRequest(
                "Payment",
                new TravelportPaymentRequest.Amount(131.8, "USD"),
                new TravelportPaymentRequest.FormOfPaymentIdentifier("FormOfPaymentPaymentCash", "fop_1", "fop_1"));

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"@type\":\"Payment\"");
        assertThat(json).contains("\"Amount\":{\"value\":131.8,\"code\":\"USD\"}");
        assertThat(json).contains("\"@type\":\"FormOfPaymentPaymentCash\"");
        assertThat(json).contains("\"FormOfPaymentRef\":\"fop_1\"");
    }

    @Test
    void deserializesAnAddFormOfPaymentResponseWithFopId() throws Exception {
        String sample = """
                {
                    "FormOfPaymentResponse": {
                        "@type": "response",
                        "reservationStatus": "Success",
                        "FormOfPayment": {
                            "@type": "FormOfPaymentPaymentCash",
                            "id": "fop_1",
                            "FormOfPaymentRef": "fopRef_1"
                        },
                        "Result": { "@type": "Result", "status": "Complete" }
                    }
                }
                """;
        ObjectMapper mapper = new ObjectMapper();
        TravelportFormOfPaymentResponse response = mapper.readValue(sample, TravelportFormOfPaymentResponse.class);

        var fop = response.FormOfPaymentResponse().FormOfPayment();
        assertThat(fop.id()).isEqualTo("fop_1");
        assertThat(fop.FormOfPaymentRef()).isEqualTo("fopRef_1");
    }

    @Test
    void serializesATicketingReservationWithAgencyFopAndPayment() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var reservation = new TravelportWorkbenchRequests.Reservation(
                "Reservation",
                null,
                null,
                List.of(new TravelportWorkbenchRequests.FormOfPayment("FormOfPaymentAgency", "fop_1", true, true)),
                List.of(new TravelportWorkbenchRequests.Payment(
                        "Payment",
                        new TravelportWorkbenchRequests.Amount(131.8, "USD"),
                        "GuenTours txn TXN-1")));

        String json = mapper.writeValueAsString(reservation);

        assertThat(json).contains("\"@type\":\"FormOfPaymentAgency\"");
        assertThat(json).contains("\"@type\":\"Payment\"");
        assertThat(json).contains("\"Amount\":{\"value\":131.8,\"code\":\"USD\"}");
        assertThat(json).contains("\"remark\":\"GuenTours txn TXN-1\"");
    }

    @Test
    void deserializesAWorkbenchCommitResponse() throws Exception {
        String sample = """
                {
                    "ReservationResponse": {
                        "@type": "response",
                        "transactionId": "49f58f5f-c443-43b4-9f5d-be405fd00a01",
                        "reservationStatus": "Success",
                        "Result": { "@type": "Result", "status": "Complete" },
                        "Identifier": { "value": "A0656EFF-FAF4-456F-B061-0161008D7C4E", "authority": "TVPT" }
                    }
                }
                """;
        ObjectMapper mapper = new ObjectMapper();
        TravelportReservationResponse response = mapper.readValue(sample, TravelportReservationResponse.class);

        var body = response.ReservationResponse();
        assertThat(body.reservationStatus()).isEqualTo("Success");
        assertThat(body.Result().status()).isEqualTo("Complete");
        assertThat(body.Identifier().value()).isEqualTo("A0656EFF-FAF4-456F-B061-0161008D7C4E");
    }
}
