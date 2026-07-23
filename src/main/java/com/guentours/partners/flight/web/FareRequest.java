package com.guentours.partners.flight.web;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record FareRequest(
        @NotNull String cabinClass,
        @NotNull BigDecimal basePrice,
        @NotNull String currency,
        @NotNull Integer baggageAllowanceKg,
        @NotNull Integer totalSeats
) {}
