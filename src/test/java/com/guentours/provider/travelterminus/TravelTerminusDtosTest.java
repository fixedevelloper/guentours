package com.guentours.provider.travelterminus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guentours.provider.travelterminus.dto.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the Travel Terminus wire shapes against real captures from the vendor's own Postman
 * collection and API docs (fetched from supplier-api.travelterminus.com), so a vendor-side field
 * rename shows up here instead of silently degrading {@link TravelTerminusClient} in production.
 */
class TravelTerminusDtosTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializesTheGenerateTokenResponse() throws Exception {
        String json = """
                {
                  "message": "Token generated successfully.",
                  "data": {
                    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhcGlLZXki",
                    "expiresIn": 86400
                  },
                  "status": true
                }
                """;

        TravelTerminusEnvelope<TravelTerminusTokenData> envelope = mapper.readValue(json,
                mapper.getTypeFactory().constructParametricType(TravelTerminusEnvelope.class, TravelTerminusTokenData.class));

        assertThat(envelope.message()).isEqualTo("Token generated successfully.");
        assertThat(envelope.data().accessToken()).startsWith("eyJhbGciOiJIUzI1NiIs");
        assertThat(envelope.data().expiresIn()).isEqualTo(86400L);
    }

    @Test
    void deserializesTheErrorEnvelope() throws Exception {
        String json = """
                {
                  "success": false,
                  "status": false,
                  "statusCode": 401,
                  "code": "invalid_credentials",
                  "message": "Invalid or missing authentication credentials.",
                  "retryable": false,
                  "errors": [ { "message": "Unauthorized", "code": "UNAUTHORIZED" } ],
                  "timestamp": "2026-06-01T10:29:02.762Z"
                }
                """;

        TravelTerminusErrorEnvelope envelope = mapper.readValue(json, TravelTerminusErrorEnvelope.class);

        assertThat(envelope.status()).isFalse();
        assertThat(envelope.statusCode()).isEqualTo(401);
        assertThat(envelope.code()).isEqualTo("invalid_credentials");
        assertThat(envelope.retryable()).isFalse();
        assertThat(envelope.errors()).hasSize(1);
        assertThat(envelope.errors().get(0).code()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void deserializesTheRealSandboxErrorEnvelopeShapeUsingErrorCodeInsteadOfCode() throws Exception {
        // Real capture from the Stage sandbox (POST /api/auth/generate-token with bad credentials):
        // uses "errorCode", not the "code" field name the docs example shows.
        String json = """
                {
                  "success": false,
                  "statusCode": 400,
                  "errorCode": "BAD_REQUEST",
                  "message": "Api key must be uuid.",
                  "retryable": false,
                  "errors": [
                    { "field": "apiKey", "message": "Api key must be uuid.", "code": "BAD_REQUEST" },
                    { "field": "secretKey", "message": "Secret key must be at least 8 characters.", "code": "BAD_REQUEST" }
                  ],
                  "timestamp": "2026-08-28T17:53:22.084Z"
                }
                """;

        TravelTerminusErrorEnvelope envelope = mapper.readValue(json, TravelTerminusErrorEnvelope.class);

        assertThat(envelope.code()).isEqualTo("BAD_REQUEST");
        assertThat(envelope.statusCode()).isEqualTo(400);
        assertThat(envelope.errors()).hasSize(2);
    }

    @Test
    void deserializesARouteUpdateSearchResponseWithAbbreviatedFareFieldNames() throws Exception {
        // Real capture from the Streaming Search Postman example (route_update event payload).
        String json = """
                {
                  "status": "success",
                  "hashReqKey": "qqtRPydoSC2yIbLL",
                  "searchReqId": "ec91fe0c-3bf4-4e55-ad2e-ef0f890d9a02",
                  "meta": { "totalResults": 66, "currency": "USD" },
                  "route": [
                    {
                      "isRefundable": false,
                      "isLcc": true,
                      "totalDuration": ["5h 40m"],
                      "totalInterval": ["0h 0m"],
                      "flightSegments": [[
                        {
                          "airlineCode": "AI",
                          "airlineName": "Air India",
                          "cabinClass": "Economy",
                          "bookingCode": "V",
                          "flightNumber": "882",
                          "segmentDuration": "1h 50m",
                          "departure": [ { "code": "AMD", "city": "Ahmedabad", "date": "2026-06-12", "time": "3:55 PM", "terminal": "2" } ],
                          "arrival":   [ { "code": "DEL", "city": "New Delhi", "date": "2026-06-12", "time": "5:45 PM", "terminal": "3" } ],
                          "segmentInterval": "0h 0m"
                        }
                      ]],
                      "fare": [
                        {
                          "fareQuote": "",
                          "fareType": "PUBLIC",
                          "perAdtBaseFare": 218.12,
                          "perAdtTax": 101.61,
                          "totalFare": 947.27,
                          "branchCurrency": "USD",
                          "walletPoints": 947.27
                        }
                      ],
                      "flightObject": { "routeId": "0bc2a726e14043901e802b45c80d01fc", "solutionId": ["abc"] }
                    }
                  ],
                  "message": "Flights fetched successfully."
                }
                """;

        TravelTerminusSearchResponse response = mapper.readValue(json, TravelTerminusSearchResponse.class);

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.route()).hasSize(1);
        var route = response.route().get(0);
        assertThat(route.isLcc()).isTrue();
        var segment = route.flightSegments().get(0).get(0);
        assertThat(segment.airlineCode()).isEqualTo("AI");
        assertThat(segment.departure().get(0).code()).isEqualTo("AMD");
        assertThat(segment.departure().get(0).time()).isEqualTo("3:55 PM");
        assertThat(segment.arrival().get(0).code()).isEqualTo("DEL");
        assertThat(route.fare().get(0).perAdtBaseFare()).isEqualTo(218.12);
        assertThat(route.fare().get(0).totalFare()).isEqualTo(947.27);
        assertThat(route.flightObject().get("routeId").asText()).isEqualTo("0bc2a726e14043901e802b45c80d01fc");
    }

    @Test
    void fareAcceptsBothTheAbbreviatedAndSpelledOutFieldNames() throws Exception {
        // The hand-written docs example uses perAdultBaseFare/perAdultTax while the real captured
        // response uses perAdtBaseFare/perAdtTax - @JsonAlias on TravelTerminusFare must accept both.
        String json = """
                { "fareType": "Spice Saver", "perAdultBaseFare": 44.05, "perAdultTax": 23.83, "totalFare": 230.2 }
                """;

        TravelTerminusFare fare = mapper.readValue(json, TravelTerminusFare.class);

        assertThat(fare.perAdtBaseFare()).isEqualTo(44.05);
        assertThat(fare.perAdtTax()).isEqualTo(23.83);
        assertThat(fare.totalFare()).isEqualTo(230.2);
    }

    @Test
    void deserializesABrandedFareSplitJourneyResponse() throws Exception {
        String json = """
                {
                  "message": "Branded fare details fetched successfully.",
                  "data": {
                    "status": "success",
                    "searchReqId": "eac09208-a444",
                    "searchObject": { "routeId": "RT-1", "pCode": "xyz" },
                    "brandedFares": [
                      { "departure": "AMD", "arrival": "DEL", "fare": [ { "fareType": "Instant", "totalFare": 39.37, "flightObject": { "brandedFareId": ["59504694"] } } ] },
                      { "departure": "DEL", "arrival": "AMD", "fare": [ { "fareType": "Retail", "totalFare": 41.0, "flightObject": { "brandedFareId": ["59504697"] } } ] }
                    ]
                  }
                }
                """;

        TravelTerminusEnvelope<TravelTerminusBrandedFareResponse> envelope = mapper.readValue(json,
                mapper.getTypeFactory().constructParametricType(TravelTerminusEnvelope.class, TravelTerminusBrandedFareResponse.class));

        var data = envelope.data();
        assertThat(data.brandedFares()).hasSize(2);
        assertThat(data.brandedFares().get(0).fare().get(0).flightObject().get("brandedFareId").get(0).asText())
                .isEqualTo("59504694");
        assertThat(data.searchObject().get("routeId").asText()).isEqualTo("RT-1");
    }

    @Test
    void deserializesARevalidateResponseWithBookingRequiredValidation() throws Exception {
        String json = """
                {
                  "message": "Revalidate route fetched successfully.",
                  "data": {
                    "status": "success",
                    "searchReqId": "a7d4790e-c7d2-4500-bb98-c725f28f2161",
                    "hashReqKey": "YpjGG5FQ9alBeM4SaVO7gHIml09",
                    "bookingRequiredValidation": {
                      "passportRequired": false,
                      "isGSTRequired": false,
                      "gstAllowed": true,
                      "isPanRequired": { "adult": false, "child": false, "infant": false },
                      "isPassportImageRequired": false,
                      "allowPassportInsteadOfPan": true,
                      "seatAncillaryRequiresPassengers": false
                    },
                    "route": {
                      "isRefundable": true,
                      "airlineType": "GDS",
                      "fare": [ { "totalFare": 166.37, "branchCurrency": "USD" } ],
                      "flightSegments": [[ { "airlineName": "IndiGo", "flightNumber": "7018", "noOfSeatAvailable": 9 } ]],
                      "flightObject": { "solutionId": ["Encrypted"], "fareId": "R5054097970737744199" }
                    }
                  }
                }
                """;

        TravelTerminusEnvelope<TravelTerminusRevalidateResponse> envelope = mapper.readValue(json,
                mapper.getTypeFactory().constructParametricType(TravelTerminusEnvelope.class, TravelTerminusRevalidateResponse.class));

        var data = envelope.data();
        assertThat(data.status()).isEqualTo("success");
        assertThat(data.bookingRequiredValidation().allowPassportInsteadOfPan()).isTrue();
        assertThat(data.bookingRequiredValidation().isPanRequired().adult()).isFalse();
        assertThat(data.route().fare().get(0).totalFare()).isEqualTo(166.37);
        assertThat(data.route().flightSegments().get(0).get(0).noOfSeatAvailable()).isEqualTo(9);
        assertThat(data.route().flightObject().get("fareId").asText()).isEqualTo("R5054097970737744199");
    }

    @Test
    void deserializesASuccessfulBookResponse() throws Exception {
        String json = """
                {
                  "message": "Flight booked successfully.",
                  "data": {
                    "status": "success",
                    "error": false,
                    "currency": "USD",
                    "orderAmount": 43.3,
                    "walletPoints": 27.01,
                    "bookingRefId": "1K7K7I8828",
                    "orderDetails": [ { "orderStatus": "Confirmed", "pnr": "TEST67" } ]
                  }
                }
                """;

        TravelTerminusEnvelope<TravelTerminusBookResponse> envelope = mapper.readValue(json,
                mapper.getTypeFactory().constructParametricType(TravelTerminusEnvelope.class, TravelTerminusBookResponse.class));

        var data = envelope.data();
        assertThat(data.error()).isFalse();
        assertThat(data.bookingRefId()).isEqualTo("1K7K7I8828");
        assertThat(data.orderDetails().get(0).orderStatus()).isEqualTo("Confirmed");
        assertThat(data.orderDetails().get(0).pnr()).isEqualTo("TEST67");
    }

    @Test
    void deserializesAFailedBookResponseWithHttp200AndErrorTrue() throws Exception {
        // Book can fail with a 200 OK - error must be read from the body, not the HTTP status.
        String json = """
                {
                  "message": "There is no flight available.",
                  "data": {
                    "status": "error",
                    "error": true,
                    "currency": "USD",
                    "orderAmount": 43.3,
                    "walletPoints": 43.3,
                    "orderDetails": [ { "message": "Session Timeout. Please start new search", "bookingRefId": "107C7C8D2D", "orderStatus": "Failed", "pnr": null } ]
                  }
                }
                """;

        TravelTerminusEnvelope<TravelTerminusBookResponse> envelope = mapper.readValue(json,
                mapper.getTypeFactory().constructParametricType(TravelTerminusEnvelope.class, TravelTerminusBookResponse.class));

        assertThat(envelope.data().error()).isTrue();
        assertThat(envelope.data().orderDetails().get(0).orderStatus()).isEqualTo("Failed");
        assertThat(envelope.data().orderDetails().get(0).pnr()).isNull();
    }

    @Test
    void deserializesOrderDetailsWithPipeDelimitedTicketNumbers() throws Exception {
        String json = """
                {
                  "message": "Order details fetched successfully.",
                  "data": {
                    "status": "success",
                    "bookingRefId": "1X7H7H9H2X",
                    "bookingStatus": "Confirmed",
                    "pnr": "TEST43 | TEST24",
                    "flightTicketNo": "0057123456789 | 0057123456790",
                    "totalAmount": "24768.01",
                    "currencyCode": "INR"
                  }
                }
                """;

        TravelTerminusEnvelope<TravelTerminusOrderDetailsResponse> envelope = mapper.readValue(json,
                mapper.getTypeFactory().constructParametricType(TravelTerminusEnvelope.class, TravelTerminusOrderDetailsResponse.class));

        assertThat(envelope.data().bookingStatus()).isEqualTo("Confirmed");
        assertThat(envelope.data().flightTicketNo()).contains("|");
    }

    @Test
    void serializesAFullCancellationRequest() throws Exception {
        TravelTerminusCancelRequest request = TravelTerminusCancelRequest.fullCancellation(
                "TT127012928N", "Carrier cancelled flight", "115.24.61.12");

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"requestType\":1");
        assertThat(json).contains("\"cancellationType\":2");
        assertThat(json).contains("\"bookingRefId\":\"TT127012928N\"");
    }

    @Test
    void deserializesTheCancelRequestAcceptedResponse() throws Exception {
        String json = """
                { "message": "Cancellation request submitted successfully.", "data": { "status": "success" } }
                """;

        TravelTerminusEnvelope<TravelTerminusCancelResponse> envelope = mapper.readValue(json,
                mapper.getTypeFactory().constructParametricType(TravelTerminusEnvelope.class, TravelTerminusCancelResponse.class));

        assertThat(envelope.data().status()).isEqualTo("success");
    }
}
