package com.guentours.partners.furnishedrental.web;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PropertyAvailabilityRequest(
        @NotNull LocalDate stayDate,
        @NotNull Boolean isAvailable,
        BigDecimal priceOverride
) {}
