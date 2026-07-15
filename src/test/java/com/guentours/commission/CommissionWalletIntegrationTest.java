package com.guentours.commission;

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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the commission fee is added on top of the displayed price (never deducted from the
 * provider's price) and that the exact same fee-inclusive price is what gets charged at
 * checkout, with one wallet entry recorded per booking.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CommissionWalletIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CommissionWalletService walletService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void checkoutChargesTheDisplayedFeeInclusivePriceAndRecordsOneWalletEntry() throws Exception {
        long entriesBefore = walletService.entryCount();

        String url = "http://localhost:" + port + "/api/search/flights?origin=CDG&destination=JFK&departureDate="
                + LocalDate.now().plusDays(30) + "&adults=1&cabinClass=ECONOMY";
        ResponseEntity<String> searchResponse = restTemplate.getForEntity(url, String.class);
        JsonNode offers = objectMapper.readTree(searchResponse.getBody());
        JsonNode bestQuote = offers.get(0).get("quotes").get(0);
        String offerId = bestQuote.get("offerId").asText();
        double displayedPrice = bestQuote.get("price").get("amount").asDouble();

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
        JsonNode booking = objectMapper.readTree(checkoutResponse.getBody());
        double bookingPrice = booking.get("price").get("amount").asDouble();

        assertThat(bookingPrice).isEqualTo(displayedPrice);
        assertThat(walletService.entryCount()).isEqualTo(entriesBefore + 1);
    }

    private HttpEntity<String> jsonEntity(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
