package com.guentours.partners.flight.web;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AvailabilityUpsertRequest(
        @NotNull LocalDate flightDate,
        @NotNull Integer seatsAvailable
) {}
