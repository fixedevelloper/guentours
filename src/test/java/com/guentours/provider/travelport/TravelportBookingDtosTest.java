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
    void serializesAPriceRequestMatchingTheProductionReferenceShape() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var request = new TravelportPriceRequest("OfferQueryBuildFromCatalogProductOfferings",
                new TravelportPriceRequest.BuildFromCatalogProductOfferingsRequest(
                        "BuildFromCatalogProductOfferingsRequestAir",
                        new TravelportPriceRequest.OfferingsRef("cpo_1",
                                new TravelportPriceRequest.Identifier("A0656EFF-FAF4-456F-B061-0161008D7C4E", "TVPT")),
                        List.of(new TravelportPriceRequest.CatalogProductOfferingSelection(
                                "CatalogProductOfferingSelection",
                                new TravelportPriceRequest.OfferingRef("cpo_1",
                                        new TravelportPriceRequest.Identifier("off1", "TVPT"), "cpo_1"),
                                new TravelportPriceRequest.Identifier("A0656EFF-FAF4-456F-B061-0161008D7C4E", "TVPT"),
                                List.of(new TravelportPriceRequest.ProductIdentifier("product_p0", "product_p0",
                                        new TravelportPriceRequest.Identifier("p0", "TVPT"))),
                                List.of(1))),
                        List.of(new TravelportPriceRequest.PassengerCriteria("PassengerCriteria", 1, "ADT", "psgr_1")),
                        "Structured"),
                new TravelportPriceRequest.PaymentCriteria("PaymentCriteria", "123456", "VI", true, true, true, true),
                4);

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"@type\":\"OfferQueryBuildFromCatalogProductOfferings\"");
        assertThat(json).contains("\"@type\":\"BuildFromCatalogProductOfferingsRequestAir\"");
        assertThat(json).contains("\"CatalogProductOfferingsIdentifier\":{\"id\":\"cpo_1\",\"Identifier\":{\"value\":\"A0656EFF-FAF4-456F-B061-0161008D7C4E\",\"authority\":\"TVPT\"}}");
        assertThat(json).contains("\"CatalogProductOfferingIdentifier\":{\"id\":\"cpo_1\",\"Identifier\":{\"value\":\"off1\",\"authority\":\"TVPT\"},\"CatalogProductOfferingRef\":\"cpo_1\"}");
        assertThat(json).contains("\"ProductBrandOfferingIdentifier\":{\"value\":\"A0656EFF-FAF4-456F-B061-0161008D7C4E\",\"authority\":\"TVPT\"}");
        assertThat(json).contains("\"ProductIdentifier\":[{\"id\":\"product_p0\",\"productRef\":\"product_p0\",\"Identifier\":{\"value\":\"p0\",\"authority\":\"TVPT\"}}]");
        assertThat(json).contains("\"SegmentSequence\":[1]");
        assertThat(json).contains("\"PassengerCriteria\":[{\"@type\":\"PassengerCriteria\",\"number\":1,\"passengerTypeCode\":\"ADT\",\"id\":\"psgr_1\"}]");
        assertThat(json).contains("\"FareRuleType\":\"Structured\"");
        assertThat(json).contains("\"IssuerIdentificationNumber\":\"123456\"");
        assertThat(json).contains("\"MaxNumberOfUpsellsToReturn\":4");
    }

    @Test
    void deserializesAPriceResponse() throws Exception {
        String sample = """
                {
                    "OfferListResponse": {
                        "@type": "OfferListResponse",
                        "transactionId": "51005a2b4d242ba02f73a79478a1e2b3",
                        "OfferID": [
                            { "id": "O1", "offerRef": "O1", "ContentSource": "GDS",
                              "Price": { "TotalPrice": 131.8, "CurrencyCode": { "value": "USD", "decimalPlace": 2 } } }
                        ],
                        "Result": { "@type": "Result" }
                    }
                }
                """;
        ObjectMapper mapper = new ObjectMapper();
        TravelportPriceResponse response = mapper.readValue(sample, TravelportPriceResponse.class);

        var offer = response.OfferListResponse().OfferID().get(0);
        assertThat(offer.id()).isEqualTo("O1");
        assertThat(offer.Price().TotalPrice()).isEqualTo(131.8);
        assertThat(offer.Price().CurrencyCode().value()).isEqualTo("USD");
    }

    @Test
    void deserializesAPriceErrorResponse() throws Exception {
        String sample = """
                {
                    "OfferListResponse": {
                        "@type": "OfferListResponse",
                        "transactionId": "51005a2b4d242ba02f73a79478a1e2b3",
                        "Result": {
                            "@type": "Result",
                            "Error": [
                                { "@type": "ErrorDetail", "category": "VALIDATION", "StatusCode": 400,
                                  "Message": "INVALID INPUT FORMAT", "SourceID": "API", "SourceCode": "1586" }
                            ]
                        }
                    }
                }
                """;
        ObjectMapper mapper = new ObjectMapper();
        TravelportPriceResponse response = mapper.readValue(sample, TravelportPriceResponse.class);

        assertThat(response.OfferListResponse().OfferID()).isNull();
        assertThat(response.OfferListResponse().Result().Error().get(0).Message()).isEqualTo("INVALID INPUT FORMAT");
    }

    @Test
    void serializesANewWorkbenchReservationWithOfferAndTravelers() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var reservation = new TravelportWorkbenchRequests.Reservation(
                "Reservation",
                List.of(new TravelportWorkbenchRequests.Offer("Offer", "offer_1", "offer_1", null, "GDS")),
                List.of(new TravelportWorkbenchRequests.Traveler(
                        "Traveler", "Female", "1990-05-01", "trav_1", "ADT",
                        new TravelportWorkbenchRequests.PersonName("PersonNameDetail", null, "Jane", null, "Doe"),
                        List.of(new TravelportWorkbenchRequests.Telephone(
                                "Telephone", "237", "670000000", "tel_1", "DLA", "Mobile")),
                        List.of(new TravelportWorkbenchRequests.Email("jane.doe@example.com")),
                        List.of(new TravelportWorkbenchRequests.TravelDocument(
                                "TravelDocumentDetail", "X1234567", "Passport", "2030-01-01", "FR",
                                "1990-05-01", "Female")))),
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
        assertThat(json).contains("\"@type\":\"Telephone\",\"countryAccessCode\":\"237\"");
        assertThat(json).contains("\"@type\":\"TravelDocumentDetail\",\"docNumber\":\"X1234567\"");
    }

    @Test
    void serializesAnAddOfferReferencePayload() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var request = new TravelportAddOfferRequest(
                new TravelportAddOfferRequest.OfferQueryBuildFromCatalogProductOfferings(
                        "OfferQueryBuildFromCatalogProductOfferings",
                        new TravelportAddOfferRequest.PaymentCriteria("PaymentCriteria", true, true, true, true),
                        new TravelportAddOfferRequest.BuildFromCatalogProductOfferingsRequest(
                                "BuildFromCatalogProductOfferingsRequestAir",
                                new TravelportAddOfferRequest.OfferingsRef("cpo_1", null),
                                List.of(new TravelportAddOfferRequest.CatalogProductOfferingSelection(
                                        "CatalogProductOfferingSelection",
                                        new TravelportAddOfferRequest.OfferingRef("cpo_1", null, "cpo_1"),
                                        null,
                                        null,
                                        List.of(1, 2)))),
                        4));

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"@type\":\"OfferQueryBuildFromCatalogProductOfferings\"");
        assertThat(json).contains("\"@type\":\"BuildFromCatalogProductOfferingsRequestAir\"");
        assertThat(json).contains("\"@type\":\"PaymentCriteria\",\"agencyAccountInd\":true");
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
                "FormOfPaymentCash", "formOfPayment_1", "formOfPayment_1", true, true, null, "GuenTours txn TXN-1");

        String json = mapper.writeValueAsString(fop);

        assertThat(json).contains("\"@type\":\"FormOfPaymentCash\"");
        assertThat(json).contains("\"id\":\"formOfPayment_1\"");
        assertThat(json).contains("\"FormOfPaymentRef\":\"formOfPayment_1\"");
        assertThat(json).contains("\"reservationFOPInd\":true");
        assertThat(json).contains("\"FreeText\":\"GuenTours txn TXN-1\"");
        assertThat(json).doesNotContain("\"Comment\"");
    }

    @Test
    void serializesAnAddPaymentRequest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var request = new TravelportPaymentRequest(
                "Payment",
                "payment_1",
                new TravelportPaymentRequest.Amount("USD", 2, "Charged", 131.8),
                new TravelportPaymentRequest.FormOfPaymentIdentifier("fop_1", "fop_1"));

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"@type\":\"Payment\"");
        assertThat(json).contains("\"id\":\"payment_1\"");
        assertThat(json).contains("\"Amount\":{\"code\":\"USD\",\"minorUnit\":2,\"currencySource\":\"Charged\",\"value\":131.8}");
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

    @Test
    void serializesACommitRequestMatchingTheProductionReferenceShape() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var request = new TravelportCommitRequest(true, true, true, false, true, true,
                "AcceptOfferPriceDifference", "GUENS TRAVEL", true);

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"scheduleChangeAcceptedInd\":true");
        assertThat(json).contains("\"errorWhenOfferPriceCancelledInd\":true");
        assertThat(json).contains("\"inhibitResidualDocumentIssuanceInd\":true");
        assertThat(json).contains("\"enableTwoStepCommitInd\":false");
        assertThat(json).contains("\"overrideMCTInd\":true");
        assertThat(json).contains("\"errorWhenScheduleChangesInd\":true");
        assertThat(json).contains("\"scheduleChangeReprice\":\"AcceptOfferPriceDifference\"");
        assertThat(json).contains("\"ReceivedFrom\":\"GUENS TRAVEL\"");
        assertThat(json).contains("\"errorWhenOfferPriceChangesInd\":true");
    }
}
