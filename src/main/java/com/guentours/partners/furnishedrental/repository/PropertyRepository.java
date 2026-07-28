package com.guentours.partners.furnishedrental.repository;

import com.guentours.partners.furnishedrental.domain.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, String> {
    Page<Property> findByPartnerId(String partnerId, Pageable pageable);
    @Query("""
        SELECT p FROM Property p
        WHERE p.status = com.guentours.partners.furnishedrental.domain.ListingStatus.ACTIVE
        AND p.city = :city
        AND p.maxGuests >= :guests
        """)
    List<Property> findActiveByCityAndCapacity(String city, int guests);
}
