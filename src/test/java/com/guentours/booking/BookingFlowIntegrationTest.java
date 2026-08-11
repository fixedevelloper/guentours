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

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

        String contactEmail = "traveler+%d@example.com".formatted(System.nanoTime());
        String checkoutBody = """
                {
                  "offerId": "%s",
                  "offerType": "FLIGHT",
                  "contactEmail": "%s",
                  "contactFullName": "Jane Traveler",
                  "contactPhone": "+33600000000",
                  "travelers": [{"fullName": "Jane Traveler", "dateOfBirth": "1990-01-01", "nationality": "US", "passportNumber": "X1234567", "type": "ADULT"}]
                }
                """.formatted(offerId, contactEmail);

        ResponseEntity<String> checkoutResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/bookings/checkout", jsonEntity(checkoutBody), String.class);
        assertThat(checkoutResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String bookingId = objectMapper.readTree(checkoutResponse.getBody()).get("id").asText();
        assertThat(objectMapper.readTree(checkoutResponse.getBody()).get("status").asText()).isEqualTo("PENDING_HOLD");

        URI bookingUri = withEmail("http://localhost:" + port + "/api/bookings/" + bookingId, contactEmail);
        URI ticketsUri = withEmail("http://localhost:" + port + "/api/tickets/booking/" + bookingId, contactEmail);

        // The provider hold now completes off-thread after checkout returns - wait for it before
        // attempting payment, since PaymentService rejects paying a booking still in PENDING_HOLD.
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ResponseEntity<String> bookingResponse = restTemplate.getForEntity(bookingUri, String.class);
            assertThat(objectMapper.readTree(bookingResponse.getBody()).get("status").asText()).isEqualTo("PENDING_PAYMENT");
        });

        String paymentBody = """
                {
                  "bookingId": "%s",
                  "paymentMethod": "CARD",
                  "countryCode": "CM",
                  "countryCurrency": "XAF",
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
            ResponseEntity<String> bookingResponse = restTemplate.getForEntity(bookingUri, String.class);
            assertThat(objectMapper.readTree(bookingResponse.getBody()).get("status").asText()).isEqualTo("CONFIRMED");
        });

        // Ticket rows are written by a separate async event listener reacting to the same
        // BookingConfirmedEvent that flipped the booking to CONFIRMED above (see ETicketService) -
        // now that it also renders/uploads a PDF (real Thymeleaf + PDF rendering, not just a JPA
        // save), it can genuinely still be running a moment after the booking itself already shows
        // CONFIRMED, so this polls instead of asserting immediately.
        java.util.concurrent.atomic.AtomicReference<JsonNode> ticketsRef = new java.util.concurrent.atomic.AtomicReference<>();
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ResponseEntity<String> ticketsResponse = restTemplate.getForEntity(ticketsUri, String.class);
            assertThat(ticketsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode tickets = objectMapper.readTree(ticketsResponse.getBody());
            assertThat(tickets.size()).isGreaterThan(0);
            ticketsRef.set(tickets);
        });
        JsonNode firstTicket = ticketsRef.get().get(0);
        assertThat(firstTicket.get("ticketNumber").asText()).isNotBlank();
        assertThat(firstTicket.get("bookingId").asText()).isEqualTo(bookingId);
        assertThat(firstTicket.get("document").asText()).contains("ELECTRONIC TICKET", contactEmail);

        // A guest supplying the wrong contact email must not be able to read someone else's e-tickets.
        URI ticketsUriWrongEmail = withEmail("http://localhost:" + port + "/api/tickets/booking/" + bookingId,
                "someone-else@example.com");
        assertThat(restTemplate.getForEntity(ticketsUriWrongEmail, String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * Builds a plain {@link URI} (not a String passed to RestTemplate's template-expanding
     * overload) so the already-percent-encoded email is used verbatim - the query string needs
     * form-encoding rules (URLEncoder: "+" -> "%2B") since that's how the servlet container
     * decodes @RequestParam values, but Spring's own UriComponentsBuilder.encode() follows
     * RFC 3986 instead, which leaves a literal "+" alone and lets the server misread it as a space.
     */
    private URI withEmail(String url, String contactEmail) {
        return URI.create(url + "?email=" + URLEncoder.encode(contactEmail, StandardCharsets.UTF_8));
    }

    private org.springframework.http.HttpEntity<String> jsonEntity(String body) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return new org.springframework.http.HttpEntity<>(body, headers);
    }
}
