package com.guentours.provider.travelport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the shape of Travelport's Stays Create Hotel Reservation (full payload) request against a
 * verified real captured sample: {@code ReservationDetail} carries its own {@code Offer}
 * ({@code ProductHospitality} + {@code Price} + rate-code detail), a {@code Payment}/
 * {@code FormOfPayment} pair (Travelport requires a guarantee card even though the real customer
 * payment is collected separately), and the {@code Traveler}(s) - distinct from the flights
 * workbench's {@code Reservation} shape an earlier, unverified version of this DTO reused.
 */
class TravelportHotelReservationRequestTest {

    @Test
    void serializesAHotelReservationWithOfferPaymentAndTraveler() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        var product = new TravelportHotelReservationRequest.Product(
                "ProductHospitality", "APB174", "1", 1,
                new TravelportHotelReservationRequest.PropertyKey("PropertyKey", "2772", "HH"),
                new TravelportHotelReservationRequest.DateRange("2026-09-11", "2026-09-15"));
        var price = new TravelportHotelReservationRequest.PriceDetail(
                "PriceDetail", new TravelportHotelReservationRequest.CurrencyCode("EUR"), 897.25, 152.56, 765.0);
        var offer = new TravelportHotelReservationRequest.Offer(
                "Offer", new TravelportHotelReservationRequest.Identifier("TVPT"), List.of(product), price, null);

        var payment = new TravelportHotelReservationRequest.Payment(
                "Payment", new TravelportHotelReservationRequest.Amount("EUR", 765.0), false, true);

        var paymentCard = new TravelportHotelReservationRequest.PaymentCard(
                "PaymentCardDetail", "0825", "Credit", "VI", "Frank Sinatra",
                new TravelportHotelReservationRequest.CardNumber("CardNumber", "4111111111111111"),
                new TravelportHotelReservationRequest.SeriesCode("SeriesCode", "123"),
                null,
                new TravelportHotelReservationRequest.Address(
                        "AddressDetail", null, "125 Billing Address Street", null, "Claremont", null,
                        new TravelportHotelReservationRequest.StateProv("CA", null),
                        new TravelportHotelReservationRequest.Country("US", null), "91711-3323"),
                List.of(new TravelportHotelReservationRequest.Telephone(
                        "TelephoneDetail", null, null, "1231234", null)),
                List.of(new TravelportHotelReservationRequest.Email("smith@example.com")));
        var formOfPayment = new TravelportHotelReservationRequest.FormOfPayment("FormOfPaymentPaymentCard", paymentCard);

        var traveler = new TravelportHotelReservationRequest.Traveler(
                "Traveler",
                new TravelportHotelReservationRequest.TravelerPersonName("PersonName", "Gabriel", "Deaconu", "Mr"),
                List.of(new TravelportHotelReservationRequest.Telephone(
                        "TelephoneDetail", "91", "011", "9891766469", "DL")),
                List.of(new TravelportHotelReservationRequest.Email("test@travelport.com")));

        var reservationDetail = new TravelportHotelReservationRequest.ReservationDetail(
                List.of(offer), List.of(payment), List.of(formOfPayment), List.of(traveler));

        String json = mapper.writeValueAsString(new TravelportHotelReservationRequest(reservationDetail));

        assertThat(json).contains("\"ReservationDetail\":{");
        assertThat(json).contains("\"bookingCode\":\"APB174\"");
        assertThat(json).contains("\"propertyCode\":\"2772\"");
        assertThat(json).contains("\"chainCode\":\"HH\"");
        assertThat(json).contains("\"@type\":\"FormOfPaymentPaymentCard\"");
        assertThat(json).contains("\"CardHolderName\":\"Frank Sinatra\"");
        assertThat(json).contains("\"PlainText\":\"4111111111111111\"");
        assertThat(json).contains("\"guaranteeInd\":false");
        assertThat(json).contains("\"depositInd\":true");
        assertThat(json).contains("\"Given\":\"Gabriel\"");
        assertThat(json).contains("\"Surname\":\"Deaconu\"");
        assertThat(json).doesNotContain("\"passengerTypeCode\"");
    }
}
