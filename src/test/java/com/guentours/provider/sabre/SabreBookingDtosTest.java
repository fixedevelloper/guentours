package com.guentours.provider.sabre;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the shape of the Revalidate Itinerary request (an OTA_AirLowFareSearchRQ that
 * pins exact flights via per-OD TPA_Extensions) and the Booking Management API
 * createBooking/cancelBooking request/response DTOs, per Sabre's official
 * AirBookingWorkflow (search -> revalidate -> createBooking).
 */
class SabreBookingDtosTest {

    @Test
    void serializesARevalidateRequestPinningTheExactFlight() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var flight = new SabreOfferSearchRequest.Flight(
                575,
                "2026-09-11T14:20:00",
                "2026-09-11T16:20:00",
                "A",
                new SabreOfferSearchRequest.Location("WAW"),
                new SabreOfferSearchRequest.Location("SPU"),
                new SabreOfferSearchRequest.Airline("LO", "LO"));
        var od = new SabreOfferSearchRequest.OriginDestinationInformation(
                "2026-09-11T14:20:00",
                new SabreOfferSearchRequest.Location("WAW"),
                new SabreOfferSearchRequest.Location("SPU"),
                new SabreOfferSearchRequest.OriginDestinationTpaExtensions(List.of(flight)));
        var rq = new SabreOfferSearchRequest.OTA_AirLowFareSearchRQ(
                "5",
                new SabreOfferSearchRequest.POS(List.of(new SabreOfferSearchRequest.Source(
                        "XXXX",
                        new SabreOfferSearchRequest.RequestorID("1", "1",
                                new SabreOfferSearchRequest.CompanyName("TN"))))),
                List.of(od),
                null,
                new SabreOfferSearchRequest.TravelerInfoSummary(
                        List.of(new SabreOfferSearchRequest.AirTravelerAvail(List.of(
                                new SabreOfferSearchRequest.PassengerTypeQuantity("ADT", 1)))),
                        new SabreOfferSearchRequest.PriceRequestInformation("USD")),
                new SabreOfferSearchRequest.TPA_Extensions(new SabreOfferSearchRequest.IntelliSellTransaction(
                        new SabreOfferSearchRequest.RequestType("REVALIDATE"))));

        String json = mapper.writeValueAsString(new SabreOfferSearchRequest(rq));

        assertThat(json).contains("\"TPA_Extensions\":{\"Flight\":[{");
        assertThat(json).contains("\"Number\":575");
        assertThat(json).contains("\"Airline\":{\"Marketing\":\"LO\",\"Operating\":\"LO\"}");
        assertThat(json).contains("\"IntelliSellTransaction\":{\"RequestType\":{\"Name\":\"REVALIDATE\"}}");
        assertThat(json).doesNotContain("TravelPreferences");
    }

    @Test
    void serializesACreateBookingRequestWithOfferTravelersAndContact() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var request = new SabreCreateBookingRequest(
                new SabreCreateBookingRequest.FlightOffer("offer-123"),
                List.of(new SabreCreateBookingRequest.Traveler("Jane", "Doe", "1990-05-01", "ADT")),
                new SabreCreateBookingRequest.ContactInfo(List.of("jane.doe@example.com")));

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"flightOffer\":{\"offerId\":\"offer-123\"}");
        assertThat(json).contains("\"givenName\":\"Jane\"");
        assertThat(json).contains("\"surname\":\"Doe\"");
        assertThat(json).contains("\"birthDate\":\"1990-05-01\"");
        assertThat(json).contains("\"passengerCode\":\"ADT\"");
        assertThat(json).contains("\"emails\":[\"jane.doe@example.com\"]");
    }

    @Test
    void deserializesACreateBookingResponseWithSabreAndAirlineConfirmationIds() throws Exception {
        String sample = """
                {
                    "confirmationId": "GLEBNY",
                    "flights": [
                        { "confirmationId": "PXVRUW", "flightNumber": "575", "airlineCode": "LO" }
                    ]
                }
                """;
        ObjectMapper mapper = new ObjectMapper();
        SabreCreateBookingResponse response = mapper.readValue(sample, SabreCreateBookingResponse.class);

        assertThat(response.confirmationId()).isEqualTo("GLEBNY");
        assertThat(response.flights()).hasSize(1);
        assertThat(response.flights().get(0).confirmationId()).isEqualTo("PXVRUW");
        assertThat(response.flights().get(0).airlineCode()).isEqualTo("LO");
        assertThat(response.errors()).isNull();
    }

    @Test
    void serializesACancelBookingRequest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(new SabreCancelBookingRequest("GLEBNY", true));

        assertThat(json).isEqualTo("{\"confirmationId\":\"GLEBNY\",\"cancelAll\":true}");
    }

    @Test
    void serializesASubmitPaymentRequestWithAnAgencyFormOfPayment() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var request = new SabreSubmitPaymentRequest(
                "ABC123",
                new SabreSubmitPaymentRequest.FormOfPayment("AGENCY_BSP", "F9RS"),
                "GuenTours txn TXN-1");

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"recordLocator\":\"ABC123\"");
        assertThat(json).contains("\"type\":\"AGENCY_BSP\"");
        assertThat(json).contains("\"agencyAccountCode\":\"F9RS\"");
        assertThat(json).contains("\"remark\":\"GuenTours txn TXN-1\"");
    }

    @Test
    void deserializesASubmitPaymentResponse() throws Exception {
        String sample = """
                {
                    "ticketed": true,
                    "eTicketNumbers": ["1234567890123"],
                    "status": "TICKETED"
                }
                """;
        ObjectMapper mapper = new ObjectMapper();
        SabreSubmitPaymentResponse response = mapper.readValue(sample, SabreSubmitPaymentResponse.class);

        assertThat(response.ticketed()).isTrue();
        assertThat(response.eTicketNumbers()).containsExactly("1234567890123");
        assertThat(response.status()).isEqualTo("TICKETED");
    }
}
