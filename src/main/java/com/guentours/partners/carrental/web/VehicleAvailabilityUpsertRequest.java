package com.guentours.partners.carrental.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record VehicleAvailabilityUpsertRequest(
        @NotNull LocalDate rentDate,
        @NotNull @Min(0) Integer unitsAvailable
) {}