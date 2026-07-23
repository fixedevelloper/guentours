package com.guentours.partners.flight.web;

import com.guentours.partners.flight.domain.AirlineFlight;
import com.guentours.partners.flight.domain.FlightStatus;

import java.time.LocalTime;
import java.util.Set;

// FlightResponse.java (backend) — à compléter

public record FlightResponse(
        String id,
        String flightNumber,
        String aircraftType,
        String originAirportCode,
        String destinationAirportCode,
        LocalTime departureTime,
        LocalTime arrivalTime,
        Integer durationMinutes,
        Set<Integer> operatingDays,
        FlightStatus status
) {
    public static FlightResponse from(AirlineFlight f) {
        return new FlightResponse(
                f.getId(), f.getFlightNumber(), f.getAircraftType(),
                f.getOriginAirportCode(), f.getDestinationAirportCode(),
                f.getDepartureTime(), f.getArrivalTime(), f.getDurationMinutes(),
                f.getOperatingDays(), f.getStatus()
        );
    }
}
