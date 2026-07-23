package com.guentours.partners.furnishedrental.repository;

import com.guentours.partners.furnishedrental.domain.PropertyAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PropertyAvailabilityRepository extends JpaRepository<PropertyAvailability, String> {
    List<PropertyAvailability> findByPropertyIdAndStayDateBetween(String propertyId, LocalDate from, LocalDate to);
    Optional<PropertyAvailability> findByPropertyIdAndStayDate(String propertyId, LocalDate date);
}
