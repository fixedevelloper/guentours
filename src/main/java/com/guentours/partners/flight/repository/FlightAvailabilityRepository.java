package com.guentours.partners.flight.repository;

import com.guentours.partners.flight.domain.FlightAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FlightAvailabilityRepository extends JpaRepository<FlightAvailability, String> {
    List<FlightAvailability> findByFareIdAndFlightDateBetween(String fareId, LocalDate from, LocalDate to);
    Optional<FlightAvailability> findByFareIdAndFlightDate(String fareId, LocalDate date);
}
