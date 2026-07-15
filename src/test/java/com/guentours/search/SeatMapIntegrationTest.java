package com.guentours.search;

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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercises the seat-selection step's backend: fetching a seat map for a searched flight offer. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SeatMapIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returnsAConsistentSeatMapForASearchedFlightOffer() throws Exception {
        String searchUrl = "http://localhost:" + port + "/api/search/flights?origin=CDG&destination=JFK&departureDate="
                + LocalDate.now().plusDays(30) + "&adults=1&cabinClass=ECONOMY";
        ResponseEntity<String> searchResponse = restTemplate.getForEntity(searchUrl, String.class);
        JsonNode offers = objectMapper.readTree(searchResponse.getBody());
        String offerId = offers.get(0).get("quotes").get(0).get("offerId").asText();

        ResponseEntity<String> seatMapResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/search/flights/seats?offerId=" + offerId, String.class);
        assertThat(seatMapResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode seatMap = objectMapper.readTree(seatMapResponse.getBody());
        assertThat(seatMap.get("rows").asInt()).isEqualTo(15);
        assertThat(seatMap.get("columns").size()).isEqualTo(6);
        assertThat(seatMap.get("seats").size()).isEqualTo(15 * 6);
        assertThat(seatMap.get("seats").get(0).get("seatNumber").asText()).isEqualTo("1A");

        // Same offer, second lookup: the map must be identical (deterministic, not re-randomized per request).
        ResponseEntity<String> secondLookup = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/search/flights/seats?offerId=" + offerId, String.class);
        assertThat(secondLookup.getBody()).isEqualTo(seatMapResponse.getBody());
    }

    @Test
    void returns404ForAnUnknownOfferId() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/search/flights/seats?offerId=does-not-exist", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
