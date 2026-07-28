package com.guentours.partners.furnishedrental.web;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PropertyAvailabilityUpsertRequest(
        @NotNull LocalDate stayDate,
        @NotNull Boolean isAvailable
) {}