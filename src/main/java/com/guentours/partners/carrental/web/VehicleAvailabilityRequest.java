package com.guentours.partners.carrental.web;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record VehicleAvailabilityRequest(
        @NotNull LocalDate rentDate,
        @NotNull Integer unitsAvailable
) {}
