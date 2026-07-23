package com.guentours.partners.flight.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.Set;

public record FlightRegistrationRequest(
        @NotBlank String flightNumber,
        @NotBlank String aircraftType,
        @NotBlank String originAirportCode,
        @NotBlank String destinationAirportCode,
        @NotNull LocalTime departureTime,
        @NotNull LocalTime arrivalTime,
        @NotNull Integer durationMinutes,
        @NotEmpty Set<Integer> operatingDays
) {}
