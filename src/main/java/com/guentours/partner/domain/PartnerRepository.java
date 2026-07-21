package com.guentours.partner.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartnerRepository extends JpaRepository<Partner, String> {
    boolean existsByEmail(String email);
    boolean existsByRegistrationNumber(String registrationNumber);
    Optional<Partner> findByEmail(String email);
    Page<Partner> findByStatus(PartnerStatus status, Pageable pageable);
}