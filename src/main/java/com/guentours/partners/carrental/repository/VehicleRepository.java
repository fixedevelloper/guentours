package com.guentours.partners.carrental.repository;

import com.guentours.partners.carrental.domain.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    Page<Vehicle> findByPartnerId(String partnerId, Pageable pageable);
    @Query("""
        SELECT v FROM Vehicle v
        WHERE v.status = com.guentours.partners.carrental.domain.ListingStatus.ACTIVE
        AND :city MEMBER OF v.pickupLocations
        """)
    List<Vehicle> findActiveByPickupCity(String city);
}
