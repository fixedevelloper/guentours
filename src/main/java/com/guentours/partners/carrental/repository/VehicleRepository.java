package com.guentours.partners.carrental.repository;

import com.guentours.partners.carrental.domain.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    Page<Vehicle> findByPartnerId(String partnerId, Pageable pageable);
}
