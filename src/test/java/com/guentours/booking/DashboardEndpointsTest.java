package com.guentours.booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guentours.user.domain.Role;
import com.guentours.user.domain.User;
import com.guentours.user.domain.UserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercises the customer dashboard ("my bookings") and admin dashboard read endpoints. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DashboardEndpointsTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void myBookingsRequiresAuthAndOnlyReturnsTheCallersOwnBookings() throws Exception {
        assertThat(restTemplate.getForEntity(url("/api/bookings/me"), String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        String email = "traveler+" + System.nanoTime() + "@example.com";
        register(email, "Secret123!");
        String token = login(email, "Secret123!");

        String bookingId = checkoutFlightFor(email);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/bookings/me"), org.springframework.http.HttpMethod.GET, authEntity(token), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode bookings = objectMapper.readTree(response.getBody());
        assertThat(bookings.isArray()).isTrue();
        assertThat(bookings.size()).isEqualTo(1);
        assertThat(bookings.get(0).get("id").asText()).isEqualTo(bookingId);
    }

    @Test
    void adminEndpointsRequireAdminRoleAndListEveryAccountAndBooking() throws Exception {
        String customerEmail = "traveler+" + System.nanoTime() + "@example.com";
        register(customerEmail, "Secret123!");
        String customerToken = login(customerEmail, "Secret123!");
        checkoutFlightFor(customerEmail);

        assertThat(restTemplate.exchange(url("/api/admin/bookings"), org.springframework.http.HttpMethod.GET,
                authEntity(customerToken), String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        String adminEmail = "admin+" + System.nanoTime() + "@example.com";
        User admin = new User(adminEmail,passwordEncoder.encode("Secret123!"), "Admin User", Role.ADMIN,  null);
        //admin.promoteToAdmin();
        userRepository.save(admin);
        String adminToken = login(adminEmail, "Secret123!");

        ResponseEntity<String> bookingsResponse = restTemplate.exchange(url("/api/admin/bookings"),
                org.springframework.http.HttpMethod.GET, authEntity(adminToken), String.class);
        assertThat(bookingsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode bookings = objectMapper.readTree(bookingsResponse.getBody());
        assertThat(bookings.size()).isGreaterThanOrEqualTo(1);

        ResponseEntity<String> usersResponse = restTemplate.exchange(url("/api/admin/users"),
                org.springframework.http.HttpMethod.GET, authEntity(adminToken), String.class);
        assertThat(usersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode users = objectMapper.readTree(usersResponse.getBody());
        assertThat(users.size()).isGreaterThanOrEqualTo(2);
    }

    private String checkoutFlightFor(String contactEmail) throws Exception {
        String searchUrl = url("/api/search/flights?origin=CDG&destination=JFK&departureDate="
                + LocalDate.now().plusDays(30) + "&adults=1&cabinClass=ECONOMY");
        JsonNode offers = objectMapper.readTree(restTemplate.getForEntity(searchUrl, String.class).getBody());
        String offerId = offers.get(0).get("quotes").get(0).get("offerId").asText();

        String checkoutBody = """
                {
                  "offerId": "%s",
                  "offerType": "FLIGHT",
                  "contactEmail": "%s",
                  "contactFullName": "Jane Traveler",
                  "contactPhone": "+33600000000",
                  "travelers": [{"fullName": "Jane Traveler", "dateOfBirth": "1990-01-01", "passportNumber": "X1234567", "type": "ADULT"}]
                }
                """.formatted(offerId, contactEmail);

        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/bookings/checkout"), jsonEntity(checkoutBody), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return objectMapper.readTree(response.getBody()).get("id").asText();
    }

    private void register(String email, String password) throws Exception {
        String body = """
                {"email": "%s", "fullName": "Jane Traveler", "phone": "+33600000000", "password": "%s"}
                """.formatted(email, password);
        ResponseEntity<String> response = restTemplate.postForEntity(url("/api/auth/register"), jsonEntity(body), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private String login(String email, String password) throws Exception {
        String body = """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
        ResponseEntity<String> response = restTemplate.postForEntity(url("/api/auth/login"), jsonEntity(body), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(response.getBody()).get("token").asText();
    }

    private HttpEntity<Void> authEntity(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private HttpEntity<String> jsonEntity(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
