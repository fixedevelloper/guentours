package com.guentours.booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guentours.booking.domain.Booking;
import com.guentours.booking.domain.BookingRepository;
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

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** Exercises the "pay later" deposit flow, local mobile-money payments, and hold auto-expiry. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PaymentPlanAndExpiryTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingService bookingService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void payingTheReservationFeeHoldsTheBookingThenTheFullPriceConfirmsIt() throws Exception {
        CheckedOutBooking booking = checkoutFlight("PAY_LATER");
        String bookingId = booking.id();

        // The provider hold now completes off-thread after checkout returns.
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(getBooking(booking).get("status").asText()).isEqualTo("PENDING_PAYMENT"));

        JsonNode afterCheckout = getBooking(booking);
        assertThat(afterCheckout.get("paymentPlan").asText()).isEqualTo("PAY_LATER");
        double price = afterCheckout.get("price").get("amount").asDouble();
        // A fixed reservation fee is due up front, and it is the amount to pay now.
        double reservationFee = afterCheckout.get("reservationFee").get("amount").asDouble();
        assertThat(reservationFee).isPositive();
        assertThat(afterCheckout.get("amountDue").get("amount").asDouble()).isEqualTo(reservationFee);

        JsonNode feePayment = pay(bookingId);
        assertThat(feePayment.get("status").asText()).isEqualTo("SUCCEEDED");
        assertThat(feePayment.get("amount").get("amount").asDouble()).isEqualTo(reservationFee);

        JsonNode afterFee = getBooking(booking);
        assertThat(afterFee.get("status").asText()).isEqualTo("DEPOSIT_PAID");
        // The reservation fee is NOT deducted: the full price is still due.
        double balance = afterFee.get("amountDue").get("amount").asDouble();
        assertThat(balance).isEqualTo(price);

        JsonNode balancePayment = pay(bookingId);
        assertThat(balancePayment.get("status").asText()).isEqualTo("SUCCEEDED");
        assertThat(balancePayment.get("amount").get("amount").asDouble()).isEqualTo(price);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(getBooking(booking).get("status").asText()).isEqualTo("CONFIRMED"));
    }

    @Test
    void hotelPayLaterDepositThenBalanceReachesConfirmed() throws Exception {
        CheckedOutBooking booking = checkoutHotel("PAY_LATER", 1);
        String bookingId = booking.id();

        // The provider hold now completes off-thread after checkout returns.
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(getBooking(booking).get("status").asText()).isEqualTo("PENDING_PAYMENT"));

        JsonNode afterCheckout = getBooking(booking);
        assertThat(afterCheckout.get("offerType").asText()).isEqualTo("HOTEL");

        JsonNode depositPayment = pay(bookingId);
        assertThat(depositPayment.get("status").asText()).isEqualTo("SUCCEEDED");
        assertThat(getBooking(booking).get("status").asText()).isEqualTo("DEPOSIT_PAID");

        JsonNode balancePayment = pay(bookingId);
        assertThat(balancePayment.get("status").asText()).isEqualTo("SUCCEEDED");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(getBooking(booking).get("status").asText()).isEqualTo("CONFIRMED"));
    }

    @Test
    void hotelCheckoutForMultipleRoomsPersistsQuantityAndScalesThePrice() throws Exception {
        CheckedOutBooking singleRoom = checkoutHotel("PAY_NOW", 1);
        awaitPendingPayment(singleRoom);
        // CommissionPolicy.addHotelFee is a single flat fee added once to the (already
        // quantity-multiplied) total, so per-room price = (price with 1 room - fee).
        double singleRoomPriceWithFee = getBooking(singleRoom).get("price").get("amount").asDouble();

        CheckedOutBooking threeRooms = checkoutHotel("PAY_NOW", 3);
        // The async provider hold (verifyHotelPrice + createHotelHold) must also succeed for 3
        // rooms, not just the checkout's synchronous price computation - this is what confirms
        // Travelport's hardcoded Quantity:"1" bug and the single-room availability recheck are
        // both actually fixed, not just the price shown at checkout time.
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(getBooking(threeRooms).get("status").asText()).isEqualTo("PENDING_PAYMENT"));

        JsonNode afterCheckout = getBooking(threeRooms);
        assertThat(afterCheckout.get("roomQuantity").asInt()).isEqualTo(3);
        double hotelFeeAmount = 15;
        double perRoomPrice = singleRoomPriceWithFee - hotelFeeAmount;
        double expectedThreeRoomsPrice = perRoomPrice * 3 + hotelFeeAmount;
        assertThat(afterCheckout.get("price").get("amount").asDouble())
                .isEqualTo(Math.round(expectedThreeRoomsPrice * 100) / 100.0);
    }

    @Test
    void paysWithMtnMobileMoneyAndOrangeMoney() throws Exception {
        CheckedOutBooking mtnBooking = checkoutFlight("PAY_NOW");
        awaitPendingPayment(mtnBooking);
        JsonNode mtnPayment = payWithMobileMoney(mtnBooking.id(), "+237670000001");
        assertThat(mtnPayment.get("status").asText()).isEqualTo("SUCCEEDED");
        assertThat(mtnPayment.get("paymentMethod").asText()).isEqualTo("MOBILE_MONEY");

        CheckedOutBooking orangeBooking = checkoutFlight("PAY_NOW");
        awaitPendingPayment(orangeBooking);
        JsonNode orangePayment = payWithMobileMoney(orangeBooking.id(), "+237690000002");
        assertThat(orangePayment.get("status").asText()).isEqualTo("SUCCEEDED");
        assertThat(orangePayment.get("paymentMethod").asText()).isEqualTo("MOBILE_MONEY");
    }

    @Test
    void mobileMoneyNumberEndingInZerosIsDeclined() throws Exception {
        CheckedOutBooking booking = checkoutFlight("PAY_NOW");
        awaitPendingPayment(booking);
        JsonNode payment = payWithMobileMoney(booking.id(), "+237670000000");
        assertThat(payment.get("status").asText()).isEqualTo("FAILED");
    }

    @Test
    void autoCancelsHoldsPastTheirTicketingDeadline() throws Exception {
        CheckedOutBooking checkedOut = checkoutFlight("PAY_NOW");
        // Wait for the async provider hold to finish (and stop touching the row) before this test
        // reaches in and rewrites the deadline directly, to avoid racing the background job.
        awaitPendingPayment(checkedOut);

        Booking booking = bookingRepository.findById(checkedOut.id()).orElseThrow();
        booking.markOnHold(booking.getProviderConfirmationNumber(), LocalDateTime.now().minusMinutes(1));
        bookingRepository.save(booking);

        bookingService.cancelExpiredHolds();

        assertThat(getBooking(checkedOut).get("status").asText()).isEqualTo("CANCELLED");
    }

    private void awaitPendingPayment(CheckedOutBooking booking) {
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(getBooking(booking).get("status").asText()).isEqualTo("PENDING_PAYMENT"));
    }

    /** Pairs a checked-out booking with the contact email it was made under - guest access to
     * {@code GET /api/bookings/{id}} now requires it to match. */
    private record CheckedOutBooking(String id, String contactEmail) {
    }

    private CheckedOutBooking checkoutFlight(String paymentPlan) throws Exception {
        String url = "http://localhost:" + port + "/api/search/flights?origin=CDG&destination=JFK&departureDate="
                + LocalDate.now().plusDays(30) + "&adults=1&cabinClass=ECONOMY";
        ResponseEntity<String> searchResponse = restTemplate.getForEntity(url, String.class);
        JsonNode offers = objectMapper.readTree(searchResponse.getBody());
        String offerId = offers.get(0).get("quotes").get(0).get("offerId").asText();

        String contactEmail = "traveler+%d@example.com".formatted(System.nanoTime());
        String checkoutBody = """
                {
                  "offerId": "%s",
                  "offerType": "FLIGHT",
                  "contactEmail": "%s",
                  "contactFullName": "Jane Traveler",
                  "contactPhone": "+33600000000",
                  "travelers": [{"fullName": "Jane Traveler", "dateOfBirth": "1990-01-01", "nationality": "US", "passportNumber": "X1234567", "type": "ADULT"}],
                  "paymentPlan": "%s"
                }
                """.formatted(offerId, contactEmail, paymentPlan);

        ResponseEntity<String> checkoutResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/bookings/checkout", jsonEntity(checkoutBody), String.class);
        assertThat(checkoutResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String bookingId = objectMapper.readTree(checkoutResponse.getBody()).get("id").asText();
        return new CheckedOutBooking(bookingId, contactEmail);
    }

    private CheckedOutBooking checkoutHotel(String paymentPlan, int quantity) throws Exception {
        String url = "http://localhost:" + port + "/api/search/hotels?cityCode=PAR&checkIn="
                + LocalDate.now().plusDays(30) + "&checkOut=" + LocalDate.now().plusDays(34)
                + "&adults=1&rooms=1&currency=XAF";
        ResponseEntity<String> searchResponse = restTemplate.getForEntity(url, String.class);
        JsonNode result = objectMapper.readTree(searchResponse.getBody());
        String offerId = result.get("offers").get(0).get("bestOfferId").asText();

        String contactEmail = "traveler+%d@example.com".formatted(System.nanoTime());
        String checkoutBody = """
                {
                  "offerId": "%s",
                  "offerType": "HOTEL",
                  "contactEmail": "%s",
                  "contactFullName": "Jane Traveler",
                  "contactPhone": "+33600000000",
                  "travelers": [{"fullName": "Jane Traveler", "dateOfBirth": "1990-01-01", "nationality": "US", "passportNumber": "X1234567", "type": "ADULT"}],
                  "paymentPlan": "%s",
                  "quantity": %d
                }
                """.formatted(offerId, contactEmail, paymentPlan, quantity);

        ResponseEntity<String> checkoutResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/bookings/checkout", jsonEntity(checkoutBody), String.class);
        assertThat(checkoutResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String bookingId = objectMapper.readTree(checkoutResponse.getBody()).get("id").asText();
        return new CheckedOutBooking(bookingId, contactEmail);
    }

    private JsonNode getBooking(CheckedOutBooking booking) throws Exception {
        URI uri = withEmail("http://localhost:" + port + "/api/bookings/" + booking.id(), booking.contactEmail());
        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
        return objectMapper.readTree(response.getBody());
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

    private JsonNode pay(String bookingId) throws Exception {
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
        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/payments", jsonEntity(paymentBody), String.class);
        return objectMapper.readTree(response.getBody());
    }

    private JsonNode payWithMobileMoney(String bookingId, String mobileNumber) throws Exception {
        String paymentBody = """
                {
                  "bookingId": "%s",
                  "paymentMethod": "MOBILE_MONEY",
                  "countryCode": "CM",
                  "countryCurrency": "XAF",
                  "mobileNumber": "%s"
                }
                """.formatted(bookingId, mobileNumber);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/payments", jsonEntity(paymentBody), String.class);
        return objectMapper.readTree(response.getBody());
    }

    private HttpEntity<String> jsonEntity(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
