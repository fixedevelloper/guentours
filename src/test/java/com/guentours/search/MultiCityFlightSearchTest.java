package com.guentours.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MultiCityFlightSearchTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returnsCombinedItinerariesGroupedByProvider() throws Exception {
        JsonNode itineraries = searchMultiCity();

        assertThat(itineraries.isArray()).isTrue();
        assertThat(itineraries.size()).isGreaterThan(0);

        JsonNode first = itineraries.get(0);
        assertThat(first.get("providerType").asText()).isNotBlank();
        assertThat(first.get("totalPrice").get("amount").asDouble()).isGreaterThan(0);
        JsonNode legs = first.get("legs");
        assertThat(legs.size()).isEqualTo(3);
        assertThat(legs.get(0).get("legIndex").asInt()).isEqualTo(0);
        assertThat(legs.get(0).get("origin").asText()).isEqualTo("CDG");
        assertThat(legs.get(0).get("destination").asText()).isEqualTo("JFK");
        assertThat(legs.get(0).get("offerId").asText()).isNotBlank();
        assertThat(legs.get(1).get("origin").asText()).isEqualTo("JFK");
        assertThat(legs.get(1).get("destination").asText()).isEqualTo("LAX");
        assertThat(legs.get(2).get("origin").asText()).isEqualTo("LAX");
        assertThat(legs.get(2).get("destination").asText()).isEqualTo("CDG");

        // Every itinerary in the list quotes a distinct provider, since each provider is only
        // included once it has an offer for every leg.
        long distinctProviders = itineraries.size();
        assertThat(distinctProviders).isGreaterThan(0);
    }

    @Test
    void rejectsMultiCityJourneyTypeOnTheSingleLegEndpoint() {
        String url = "http://localhost:" + port + "/api/search/flights?origin=CDG&destination=JFK&departureDate="
                + LocalDate.now().plusDays(30) + "&journeyType=MULTI_CITY";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsFewerThanTwoLegs() {
        String body = """
                {"legs": [{"origin": "CDG", "destination": "JFK", "departureDate": "%s"}]}
                """.formatted(LocalDate.now().plusDays(30));

        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/search/flights/multi-city", jsonEntity(body), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void booksEveryLegOfASelectedItineraryAsOneBookingAndConfirmsAllTickets() throws Exception {
        JsonNode itinerary = searchMultiCity().get(0);
        JsonNode legs = itinerary.get("legs");
        String legOfferIds = objectMapper.writeValueAsString(
                List.of(legs.get(0).get("offerId").asText(), legs.get(1).get("offerId").asText(),
                        legs.get(2).get("offerId").asText()));

        String checkoutBody = """
                {
                  "legOfferIds": %s,
                  "contactEmail": "traveler+%d@example.com",
                  "contactFullName": "Jane Traveler",
                  "contactPhone": "+33600000000",
                  "travelers": [{"fullName": "Jane Traveler", "dateOfBirth": "1990-01-01", "passportNumber": "X1234567", "type": "ADULT"}]
                }
                """.formatted(legOfferIds, System.nanoTime());

        ResponseEntity<String> checkoutResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/bookings/checkout/multi-city", jsonEntity(checkoutBody), String.class);
        assertThat(checkoutResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode booking = objectMapper.readTree(checkoutResponse.getBody());
        String bookingId = booking.get("id").asText();
        assertThat(booking.get("itineraryLegs").size()).isEqualTo(3);

        String paymentBody = """
                {
                  "bookingId": "%s",
                  "paymentMethod": "CARD",
                  "cardNumber": "4242424242421234",
                  "cardHolderName": "Jane Traveler",
                  "expiry": "12/30",
                  "cvv": "123"
                }
                """.formatted(bookingId);

        ResponseEntity<String> paymentResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/payments", jsonEntity(paymentBody), String.class);
        assertThat(paymentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ResponseEntity<String> bookingResponse = restTemplate.getForEntity(
                    "http://localhost:" + port + "/api/bookings/" + bookingId, String.class);
            JsonNode confirmed = objectMapper.readTree(bookingResponse.getBody());
            assertThat(confirmed.get("status").asText()).isEqualTo("CONFIRMED");
            assertThat(confirmed.get("eTicketNumbers").size()).isEqualTo(3);
        });
    }

    private JsonNode searchMultiCity() throws Exception {
        String body = """
                {
                  "legs": [
                    {"origin": "CDG", "destination": "JFK", "departureDate": "%s"},
                    {"origin": "JFK", "destination": "LAX", "departureDate": "%s"},
                    {"origin": "LAX", "destination": "CDG", "departureDate": "%s"}
                  ],
                  "adults": 1,
                  "cabinClass": "ECONOMY",
                  "currency": "EUR"
                }
                """.formatted(LocalDate.now().plusDays(30), LocalDate.now().plusDays(35), LocalDate.now().plusDays(40));

        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/search/flights/multi-city", jsonEntity(body), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(response.getBody());
    }

    private HttpEntity<String> jsonEntity(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
