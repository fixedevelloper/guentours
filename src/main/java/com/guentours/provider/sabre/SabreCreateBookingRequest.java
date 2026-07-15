package com.guentours.provider.sabre;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Request body for Sabre's Booking Management API booking-creation endpoint
 * ({@code POST /v1/trip/orders/createBooking} - endpoint confirmed by Sabre's official
 * AirBookingWorkflow, which books the flights, prices/stores the fare, and adds all
 * traveler data in a single call). {@code flightOffer.offerId} references the itinerary
 * kept alive by the preceding shop/revalidate step. Traveler/contact field names follow
 * the Booking Management API's documented camelCase conventions; identity documents
 * (passports) are not sent yet pending the exact spec for them.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record SabreCreateBookingRequest(
        FlightOffer flightOffer,
        List<Traveler> travelers,
        ContactInfo contactInfo
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record FlightOffer(String offerId) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Traveler(
            String givenName,
            String surname,
            String birthDate,
            String passengerCode
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ContactInfo(List<String> emails) {
    }
}
