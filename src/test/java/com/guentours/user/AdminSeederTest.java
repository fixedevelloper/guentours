package com.guentours.user;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the default super-admin seeded at startup (see {@link AdminSeeder}) - same
 * Spring context/config as every other test class here, so the seeder runs with the app's
 * real defaults instead of a one-off override.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminSeederTest {

    private static final String DEFAULT_ADMIN_EMAIL = "admin@guentours.com";
    private static final String DEFAULT_ADMIN_PASSWORD = "ChangeMe123!";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminSeeder adminSeeder;

    @Test
    void seedsTheDefaultSuperAdminAndItCanLogIn() {
        User admin = userRepository.findByEmailIgnoreCase(DEFAULT_ADMIN_EMAIL).orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);

        String loginBody = """
                {"email": "%s", "password": "%s"}
                """.formatted(DEFAULT_ADMIN_EMAIL, DEFAULT_ADMIN_PASSWORD);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/login", jsonEntity(loginBody), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"role\":\"ADMIN\"");
    }

    @Test
    void reseedingIsANoOpOnceTheAccountAlreadyExists() {
        long countBefore = userRepository.count();
        adminSeeder.seed();
        assertThat(userRepository.count()).isEqualTo(countBefore);
    }

    private HttpEntity<String> jsonEntity(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
