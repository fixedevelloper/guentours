package com.guentours.booking.web;

import com.guentours.provider.PassengerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record TravelerRequest(
        @NotBlank String fullName,
        LocalDate dateOfBirth,
        String passportNumber,
        @NotNull PassengerType type,
        /** Seat picked at the seat-selection checkout step (FLIGHT only); null if none was assigned. */
        String seatNumber,
        /** ISO 3166-1 alpha-2 nationality; required by some providers' flight booking APIs (e.g. Travelopro). */
        String nationality,
        /** ISO 3166-1 alpha-2 passport-issuing country; optional, requested by some flight booking APIs. */
        String passportIssueCountry,
        /** Passport expiry date; optional, requested by some flight booking APIs. */
        LocalDate passportExpiryDate,
        /**
         * Ids returned by {@code POST /api/bookings/ancillary-options} for the extras (baggage/
         * meal/seat/insurance) this traveler picked at the "additional options" checkout step;
         * null/empty if none. Price and, where relevant, the provider token needed to apply the
         * selection are resolved server-side from that cache, never trusted from the client.
         */
        List<String> selectedAncillaryIds
) {
}
