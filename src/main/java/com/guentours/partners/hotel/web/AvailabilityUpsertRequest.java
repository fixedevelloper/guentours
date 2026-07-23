package com.guentours.partners.hotel.web;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AvailabilityUpsertRequest(
        @NotNull LocalDate stayDate,
        @NotNull Integer roomsAvailable
) {}
