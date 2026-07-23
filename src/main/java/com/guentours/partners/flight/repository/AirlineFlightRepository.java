package com.guentours.partners.flight.repository;

import com.guentours.partners.flight.domain.AirlineFlight;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AirlineFlightRepository extends JpaRepository<AirlineFlight, String> {
    Page<AirlineFlight> findByPartnerId(String partnerId, Pageable pageable);
    boolean existsByPartnerIdAndFlightNumber(String partnerId, String flightNumber);
}
