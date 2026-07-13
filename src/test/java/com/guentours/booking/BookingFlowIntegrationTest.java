package com.guentours.booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Exercises the whole flow end to end against the mock provider adapters: search a
 * flight, checkout the cheapest harmonized offer (auto-provisioning the guest's
 * account), pay for it, and follow the booking through to CONFIRMED with e-tickets issued.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BookingFlowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void searchesChecksOutPaysAndConfirmsABooking() throws Exception {
        String url = "http://localhost:" + port + "/api/search/flights?origin=CDG&destination=JFK&departureDate="
                + LocalDate.now().plusDays(30) + "&adults=1&cabinClass=ECONOMY";
        ResponseEntity<String> searchResponse = restTemplate.getForEntity(url, String.class);
        assertThat(searchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode offers = objectMapper.readTree(searchResponse.getBody());
        assertThat(offers.isArray()).isTrue();
        assertThat(offers.size()).isGreaterThan(0);
        String offerId = offers.get(0).get("quotes").get(0).get("offerId").asText();

        String checkoutBody = """
                {
                  "offerId": "%s",
                  "offerType": "FLIGHT",
                  "contactEmail": "traveler+%d@example.com",
                  "contactFullName": "Jane Traveler",
                  "contactPhone": "+33600000000",
                  "travelers": [{"fullName": "Jane Traveler", "dateOfBirth": "1990-01-01", "passportNumber": "X1234567", "type": "ADULT"}]
                }
                """.formatted(offerId, System.nanoTime());

        ResponseEntity<String> checkoutResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/bookings/checkout", jsonEntity(checkoutBody), String.class);
        assertThat(checkoutResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String bookingId = objectMapper.readTree(checkoutResponse.getBody()).get("id").asText();

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
        assertThat(objectMapper.readTree(paymentResponse.getBody()).get("status").asText()).isEqualTo("SUCCEEDED");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ResponseEntity<String> bookingResponse = restTemplate.getForEntity(
                    "http://localhost:" + port + "/api/bookings/" + bookingId, String.class);
            assertThat(objectMapper.readTree(bookingResponse.getBody()).get("status").asText()).isEqualTo("CONFIRMED");
        });

        ResponseEntity<String> ticketsResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/tickets/booking/" + bookingId, String.class);
        assertThat(objectMapper.readTree(ticketsResponse.getBody()).size()).isGreaterThan(0);
    }

    private org.springframework.http.HttpEntity<String> jsonEntity(String body) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return new org.springframework.http.HttpEntity<>(body, headers);
    }
}
