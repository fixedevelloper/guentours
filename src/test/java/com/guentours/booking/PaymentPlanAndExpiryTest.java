package com.guentours.booking;

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
        String bookingId = checkoutFlight("PAY_LATER");

        JsonNode afterCheckout = getBooking(bookingId);
        assertThat(afterCheckout.get("status").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(afterCheckout.get("paymentPlan").asText()).isEqualTo("PAY_LATER");
        double price = afterCheckout.get("price").get("amount").asDouble();
        // A fixed reservation fee is due up front, and it is the amount to pay now.
        double reservationFee = afterCheckout.get("reservationFee").get("amount").asDouble();
        assertThat(reservationFee).isPositive();
        assertThat(afterCheckout.get("amountDue").get("amount").asDouble()).isEqualTo(reservationFee);

        JsonNode feePayment = pay(bookingId);
        assertThat(feePayment.get("status").asText()).isEqualTo("SUCCEEDED");
        assertThat(feePayment.get("amount").get("amount").asDouble()).isEqualTo(reservationFee);

        JsonNode afterFee = getBooking(bookingId);
        assertThat(afterFee.get("status").asText()).isEqualTo("DEPOSIT_PAID");
        // The reservation fee is NOT deducted: the full price is still due.
        double balance = afterFee.get("amountDue").get("amount").asDouble();
        assertThat(balance).isEqualTo(price);

        JsonNode balancePayment = pay(bookingId);
        assertThat(balancePayment.get("status").asText()).isEqualTo("SUCCEEDED");
        assertThat(balancePayment.get("amount").get("amount").asDouble()).isEqualTo(price);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(getBooking(bookingId).get("status").asText()).isEqualTo("CONFIRMED"));
    }

    @Test
    void hotelPayLaterDepositThenBalanceReachesConfirmed() throws Exception {
        String bookingId = checkoutHotel("PAY_LATER");

        JsonNode afterCheckout = getBooking(bookingId);
        assertThat(afterCheckout.get("status").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(afterCheckout.get("offerType").asText()).isEqualTo("HOTEL");

        JsonNode depositPayment = pay(bookingId);
        assertThat(depositPayment.get("status").asText()).isEqualTo("SUCCEEDED");
        assertThat(getBooking(bookingId).get("status").asText()).isEqualTo("DEPOSIT_PAID");

        JsonNode balancePayment = pay(bookingId);
        assertThat(balancePayment.get("status").asText()).isEqualTo("SUCCEEDED");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(getBooking(bookingId).get("status").asText()).isEqualTo("CONFIRMED"));
    }

    @Test
    void paysWithMtnMobileMoneyAndOrangeMoney() throws Exception {
        String mtnBookingId = checkoutFlight("PAY_NOW");
        JsonNode mtnPayment = payWithMobileMoney(mtnBookingId, "MTN_MOBILE_MONEY", "+237670000001");
        assertThat(mtnPayment.get("status").asText()).isEqualTo("SUCCEEDED");
        assertThat(mtnPayment.get("paymentMethod").asText()).isEqualTo("MTN_MOBILE_MONEY");

        String orangeBookingId = checkoutFlight("PAY_NOW");
        JsonNode orangePayment = payWithMobileMoney(orangeBookingId, "ORANGE_MONEY", "+237690000002");
        assertThat(orangePayment.get("status").asText()).isEqualTo("SUCCEEDED");
        assertThat(orangePayment.get("paymentMethod").asText()).isEqualTo("ORANGE_MONEY");
    }

    @Test
    void mobileMoneyNumberEndingInZerosIsDeclined() throws Exception {
        String bookingId = checkoutFlight("PAY_NOW");
        JsonNode payment = payWithMobileMoney(bookingId, "MTN_MOBILE_MONEY", "+237670000000");
        assertThat(payment.get("status").asText()).isEqualTo("FAILED");
    }

    @Test
    void autoCancelsHoldsPastTheirTicketingDeadline() throws Exception {
        String bookingId = checkoutFlight("PAY_NOW");

        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.markOnHold(booking.getProviderConfirmationNumber(), LocalDateTime.now().minusMinutes(1));
        bookingRepository.save(booking);

        bookingService.cancelExpiredHolds();

        assertThat(getBooking(bookingId).get("status").asText()).isEqualTo("CANCELLED");
    }

    private String checkoutFlight(String paymentPlan) throws Exception {
        String url = "http://localhost:" + port + "/api/search/flights?origin=CDG&destination=JFK&departureDate="
                + LocalDate.now().plusDays(30) + "&adults=1&cabinClass=ECONOMY";
        ResponseEntity<String> searchResponse = restTemplate.getForEntity(url, String.class);
        JsonNode offers = objectMapper.readTree(searchResponse.getBody());
        String offerId = offers.get(0).get("quotes").get(0).get("offerId").asText();

        String checkoutBody = """
                {
                  "offerId": "%s",
                  "offerType": "FLIGHT",
                  "contactEmail": "traveler+%d@example.com",
                  "contactFullName": "Jane Traveler",
                  "contactPhone": "+33600000000",
                  "travelers": [{"fullName": "Jane Traveler", "dateOfBirth": "1990-01-01", "passportNumber": "X1234567", "type": "ADULT"}],
                  "paymentPlan": "%s"
                }
                """.formatted(offerId, System.nanoTime(), paymentPlan);

        ResponseEntity<String> checkoutResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/bookings/checkout", jsonEntity(checkoutBody), String.class);
        assertThat(checkoutResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return objectMapper.readTree(checkoutResponse.getBody()).get("id").asText();
    }

    private String checkoutHotel(String paymentPlan) throws Exception {
        String url = "http://localhost:" + port + "/api/search/hotels?cityCode=Paris&checkIn="
                + LocalDate.now().plusDays(30) + "&checkOut=" + LocalDate.now().plusDays(34) + "&adults=1&rooms=1";
        ResponseEntity<String> searchResponse = restTemplate.getForEntity(url, String.class);
        JsonNode offers = objectMapper.readTree(searchResponse.getBody());
        String offerId = offers.get(0).get("bestOfferId").asText();

        String checkoutBody = """
                {
                  "offerId": "%s",
                  "offerType": "HOTEL",
                  "contactEmail": "traveler+%d@example.com",
                  "contactFullName": "Jane Traveler",
                  "contactPhone": "+33600000000",
                  "travelers": [{"fullName": "Jane Traveler", "dateOfBirth": "1990-01-01", "passportNumber": "X1234567", "type": "ADULT"}],
                  "paymentPlan": "%s"
                }
                """.formatted(offerId, System.nanoTime(), paymentPlan);

        ResponseEntity<String> checkoutResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/bookings/checkout", jsonEntity(checkoutBody), String.class);
        assertThat(checkoutResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return objectMapper.readTree(checkoutResponse.getBody()).get("id").asText();
    }

    private JsonNode getBooking(String bookingId) throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/bookings/" + bookingId, String.class);
        return objectMapper.readTree(response.getBody());
    }

    private JsonNode pay(String bookingId) throws Exception {
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
        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/payments", jsonEntity(paymentBody), String.class);
        return objectMapper.readTree(response.getBody());
    }

    private JsonNode payWithMobileMoney(String bookingId, String method, String mobileNumber) throws Exception {
        String paymentBody = """
                {
                  "bookingId": "%s",
                  "paymentMethod": "%s",
                  "mobileNumber": "%s"
                }
                """.formatted(bookingId, method, mobileNumber);
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
