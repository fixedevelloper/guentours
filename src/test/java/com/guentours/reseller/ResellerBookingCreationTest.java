package com.guentours.reseller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guentours.booking.domain.Booking;
import com.guentours.booking.domain.BookingRepository;
import com.guentours.booking.domain.BookingStatus;
import com.guentours.booking.domain.OfferType;
import com.guentours.booking.domain.PaymentPlan;
import com.guentours.booking.web.CheckoutRequest;
import com.guentours.booking.web.TravelerRequest;
import com.guentours.provider.PassengerType;
import com.guentours.reseller.domain.Reseller;
import com.guentours.reseller.domain.ResellerRepository;
import com.guentours.reseller.service.ResellerBookingService;
import com.guentours.reseller.web.ResellerBookingResponse;
import com.guentours.reseller.web.ResellerCheckoutRequest;
import com.guentours.security.AppUserPrincipal;
import com.guentours.user.domain.User;
import com.guentours.user.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Regression test for the race between {@code BookingService#checkout}'s async provider hold
 * ({@code completeHold}, running off-thread against the same booking row) and
 * {@code ResellerBookingService#createBookingHold} tagging that same booking with the reseller
 * that sold it. The old code re-saved the (still @Version-tracked) entity returned by checkout()
 * after mutating it in place, which could lose the optimistic-lock race against completeHold's
 * own concurrent save and throw - the exact 500 reported on POST /api/reseller/bookings.
 * Calls {@link ResellerBookingService} directly (not through the HTTP layer) since the fix itself
 * is a persistence-layer concern, not a web-layer one - this sidesteps having to also fabricate a
 * CSRF handshake for an unrelated, already-authenticated call.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ResellerBookingCreationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ResellerRepository resellerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ResellerBookingService resellerBookingService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createBookingHoldTagsTheBookingWithoutRacingTheAsyncProviderHold() throws Exception {
        long nonce = System.nanoTime();

        Reseller reseller = new Reseller(null, "Test Agency", "Test Contact",
                "reseller+" + nonce + "@example.com", "+237600000000", "REG-" + nonce,
                "Douala", "Cameroon", "PROMO" + nonce, new BigDecimal("0.05"), null);
        reseller.approve();
        reseller = resellerRepository.save(reseller);

        User user = new User("reseller-login+" + nonce + "@example.com", passwordEncoder.encode("Secret123!"),
                "Test Reseller", "+237600000000", reseller.getId());
        user = userRepository.save(user);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                new AppUserPrincipal(user), null, new AppUserPrincipal(user).getAuthorities());

        String offerId = firstFlightOfferId();
        String contactEmail = "traveler+" + nonce + "@example.com";
        CheckoutRequest checkoutRequest = new CheckoutRequest(
                offerId, OfferType.FLIGHT, contactEmail, "Jane Traveler", "+33600000000",
                List.of(new TravelerRequest("Jane Traveler", LocalDate.of(1990, 1, 1), "X1234567",
                        PassengerType.ADULT, null, "US", null, null, null)),
                PaymentPlan.PAY_NOW, null);

        ResellerBookingResponse response = resellerBookingService.createBookingHold(
                new ResellerCheckoutRequest(checkoutRequest, BigDecimal.TEN), authentication);

        assertThat(response.resellerId()).isEqualTo(reseller.getId());
        assertThat(response.status()).isEqualTo(BookingStatus.PENDING_HOLD);

        String resellerIdCaptured = reseller.getId();
        String bookingId = response.id();

        // completeHold (BookingService's async provider round trip) keeps running on its own
        // thread/transaction after createBookingHold already returned - wait for it to settle,
        // then confirm the reseller tag survived it (this is exactly what the old load-mutate-save
        // could lose, or 500 on, under the right timing).
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Optional<Booking> settled = bookingRepository.findById(bookingId);
            assertThat(settled).isPresent();
            assertThat(settled.get().getStatus()).isNotEqualTo(BookingStatus.PENDING_HOLD);
        });

        Booking finalBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(finalBooking.getResellerId()).isEqualTo(resellerIdCaptured);
        assertThat(finalBooking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    private String firstFlightOfferId() throws Exception {
        String searchUrl = "http://localhost:" + port + "/api/search/flights?origin=CDG&destination=JFK&departureDate="
                + LocalDate.now().plusDays(30) + "&adults=1&cabinClass=ECONOMY";
        ResponseEntity<String> searchResponse = restTemplate.getForEntity(searchUrl, String.class);
        assertThat(searchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode offers = objectMapper.readTree(searchResponse.getBody());
        assertThat(offers.isArray()).isTrue();
        assertThat(offers.size()).isGreaterThan(0);
        return offers.get(0).get("quotes").get(0).get("offerId").asText();
    }
}
