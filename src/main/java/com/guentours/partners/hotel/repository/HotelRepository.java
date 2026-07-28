package com.guentours.partners.hotel.repository;

import com.guentours.partners.carrental.domain.ListingStatus;
import com.guentours.partners.hotel.domain.Hotel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HotelRepository extends JpaRepository<Hotel, String> {
    Page<Hotel> findByPartnerId(String partnerId, Pageable pageable);
    List<Hotel> findByCityIgnoreCaseAndCountryIgnoreCaseAndStatus(
            String city,
            String country,
            ListingStatus status
    );
}
