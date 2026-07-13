package com.guentours.search;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MultiCityFlightSearchRequest(
        @NotEmpty @Size(min = 2, max = 6) @Valid List<FlightLeg> legs,
        @Min(1) Integer adults,
        @Min(0) Integer children,
        @Min(0) Integer infants,
        String cabinClass,
        @Pattern(regexp = "[A-Za-z]{3}", message = "currency must be a 3-letter ISO 4217 code") String currency
) {
}
