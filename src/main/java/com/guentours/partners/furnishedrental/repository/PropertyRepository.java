package com.guentours.partners.furnishedrental.repository;

import com.guentours.partners.furnishedrental.domain.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, String> {
    Page<Property> findByPartnerId(String partnerId, Pageable pageable);
}
