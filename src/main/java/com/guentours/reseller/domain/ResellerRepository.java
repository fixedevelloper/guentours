package com.guentours.reseller.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResellerRepository extends JpaRepository<Reseller, String> {
    boolean existsByEmail(String email);
    boolean existsByPromoCode(String promoCode);
    Optional<Reseller> findByPromoCodeIgnoreCase(String promoCode);
    Page<Reseller> findByStatus(ResellerStatus status, Pageable pageable);
}