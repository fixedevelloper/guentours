package com.guentours.provider.travelport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the shape of Travelport's Stays Create Hotel Reservation (full payload) request against a
 * verified real sample: the body wraps a {@code ReservationDetail} of {@code @type: Reservation}
 * carrying the cached {@code Offer} and the {@code Traveler}(s) - the same Reservation shape the
 * flights workbench uses.
 */
class TravelportHotelReservationRequestTest {

    @Test
    void serializesAHotelReservationWithOfferAndGuest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var reservation = new TravelportWorkbenchRequests.Reservation(
                "Reservation",
                List.of(new TravelportWorkbenchRequests.Offer("Offer", "offer_1", "offer_1", null, "GDS")),
                List.of(new TravelportWorkbenchRequests.Traveler(
                        "Traveler", "1990-05-01", "ADT",
                        new TravelportWorkbenchRequests.PersonName("PersonNameDetail", null, "Jane", null, "Doe"),
                        List.of(new TravelportWorkbenchRequests.Email("jane.doe@example.com")))),
                null,
                null);

        String json = mapper.writeValueAsString(new TravelportHotelReservationRequest(reservation));

        assertThat(json).contains("\"ReservationDetail\":{");
        assertThat(json).contains("\"@type\":\"Reservation\"");
        assertThat(json).contains("\"id\":\"offer_1\"");
        assertThat(json).contains("\"Given\":\"Jane\"");
        assertThat(json).contains("\"Surname\":\"Doe\"");
        assertThat(json).contains("\"passengerTypeCode\":\"ADT\"");
    }
}
