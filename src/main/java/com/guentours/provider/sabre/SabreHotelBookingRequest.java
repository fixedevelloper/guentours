package com.guentours.provider.sabre;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Request body for Sabre's Booking Management API booking-creation endpoint, hotel variant
 * ({@code POST /v1/trip/orders/createBooking} - same order endpoint used for flights, see
 * {@link SabreCreateBookingRequest}). The {@code hotels} array name and per-item shape follow
 * the Booking Management API's confirmed cancellation contract (cancelling a hotel item
 * addresses it as {@code hotels: [{ itemId }]}), so creation is expected to mirror it with
 * the {@code BookingKey} obtained from Hotel Price Check. Verify against a live CERT booking
 * before disabling mock mode.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record SabreHotelBookingRequest(
        List<Hotel> hotels,
        List<Guest> guests,
        ContactInfo contactInfo
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Hotel(String bookingKey) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Guest(String givenName, String surname) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ContactInfo(List<String> emails) {
    }
}
