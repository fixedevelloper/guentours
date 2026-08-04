package com.guentours.provider.travelport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the shape {@link TravelportClient#confirmationFrom}/{@code supplierLocatorFrom} depend on:
 * a hotel reservation's {@code Receipt} list carries two distinct kinds of entry - a
 * reservation-wide one with no {@code OfferRef} (the GDS-level locator stored as the booking's
 * confirmation number) and one tied to a specific offer via {@code OfferRef} (the supplier's own
 * locator for that offer, needed by the cancellation call alongside the confirmation). This sample
 * is a best-effort reconstruction from the flight-workbench Receipt shape already verified
 * elsewhere in this codebase (see {@link TravelportReservationResponse}'s own Javadoc) applied to
 * the Stays hotel reservation response, since no real captured hotel-reservation trace with two
 * receipts was available - re-verify against a real trace if Travelport's actual hotel response
 * differs.
 */
class TravelportReservationResponseTest {

    private static final String SAMPLE_RESPONSE = """
            {
                "ReservationResponse": {
                    "transactionId": "abc123",
                    "reservationStatus": "Success",
                    "Reservation": {
                        "locatorCode": "GDS123",
                        "Identifier": { "value": "f0c5e353-b50a-483b-9f3e-78c5333a6543", "authority": "TVPT" },
                        "Receipt": [
                            {
                                "Identifier": { "value": "receipt-1", "authority": "TVPT" },
                                "Confirmation": { "Locator": { "source": "GDS", "sourceContext": "1G", "value": "GDS123" } }
                            },
                            {
                                "Identifier": { "value": "receipt-2", "authority": "TVPT" },
                                "OfferRef": ["o1"],
                                "Confirmation": { "Locator": { "source": "Supplier", "sourceContext": "HH", "value": "SUP-9F8E" } }
                            }
                        ]
                    }
                }
            }
            """;

    @Test
    void deserializesBothTheReservationLevelAndOfferLevelReceipts() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        TravelportReservationResponse response = mapper.readValue(SAMPLE_RESPONSE, TravelportReservationResponse.class);

        var reservation = response.ReservationResponse().Reservation();
        assertThat(reservation.Receipt()).hasSize(2);

        var reservationLevel = reservation.Receipt().get(0);
        assertThat(reservationLevel.OfferRef()).isNull();
        assertThat(reservationLevel.Confirmation().Locator().value()).isEqualTo("GDS123");

        var offerLevel = reservation.Receipt().get(1);
        assertThat(offerLevel.OfferRef()).containsExactly("o1");
        assertThat(offerLevel.Confirmation().Locator().value()).isEqualTo("SUP-9F8E");
    }
}
