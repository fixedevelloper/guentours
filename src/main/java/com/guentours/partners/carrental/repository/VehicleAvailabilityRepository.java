package com.guentours.partners.carrental.repository;

import com.guentours.partners.carrental.domain.VehicleAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VehicleAvailabilityRepository extends JpaRepository<VehicleAvailability, String> {
    List<VehicleAvailability> findByVehicleIdAndRentDateBetween(String vehicleId, LocalDate from, LocalDate to);
    Optional<VehicleAvailability> findByVehicleIdAndRentDate(String vehicleId, LocalDate date);
}
