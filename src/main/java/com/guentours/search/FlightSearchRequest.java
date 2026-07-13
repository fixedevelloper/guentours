package com.guentours.search;

import com.guentours.provider.JourneyType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record FlightSearchRequest(
        @NotBlank String origin,
        @NotBlank String destination,
        @NotNull @FutureOrPresent LocalDate departureDate,
        LocalDate returnDate,
        @Min(1) Integer adults,
        @Min(0) Integer children,
        @Min(0) Integer infants,
        JourneyType journeyType,
        String cabinClass,
        @Pattern(regexp = "[A-Za-z]{3}", message = "currency must be a 3-letter ISO 4217 code") String currency
) {
}
