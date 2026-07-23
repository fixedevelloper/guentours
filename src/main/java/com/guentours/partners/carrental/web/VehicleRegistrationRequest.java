package com.guentours.partners.carrental.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record VehicleRegistrationRequest(
        @NotBlank String brand,
        @NotBlank String model,
        @NotNull Integer year,
        @NotBlank String category,
        @NotBlank String transmission,
        @NotNull Integer seats,
        @NotNull Boolean airConditioning,
        @NotNull BigDecimal pricePerDay,
        @NotBlank String currency,
        @NotNull Integer unitsCount,
        List<String> pickupLocations
) {}
