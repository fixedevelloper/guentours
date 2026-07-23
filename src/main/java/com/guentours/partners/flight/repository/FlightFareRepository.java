package com.guentours.partners.flight.repository;

import com.guentours.partners.flight.domain.FlightFare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlightFareRepository extends JpaRepository<FlightFare, String> {
    List<FlightFare> findByFlightId(String flightId);
}
