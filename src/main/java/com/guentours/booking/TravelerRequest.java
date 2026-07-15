package com.guentours.booking;

import com.guentours.provider.PassengerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TravelerRequest(
        @NotBlank String fullName,
        LocalDate dateOfBirth,
        String passportNumber,
        @NotNull PassengerType type,
        /** Seat picked at the seat-selection checkout step (FLIGHT only); null if none was assigned. */
        String seatNumber
) {
}
