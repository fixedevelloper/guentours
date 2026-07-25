package com.guentours.reseller.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResellerCommissionRepository extends JpaRepository<ResellerCommissionEntry, String> {

    /**
     * Vérifie si une commission a déjà été générée pour une réservation donnée.
     * Indispensable pour garantir l'idempotence du crédit de commission.
     */
    boolean existsByBookingId(String bookingId);

    /**
     * Recherche la commission associée à une réservation spécifique.
     */
    Optional<ResellerCommissionEntry> findByBookingId(String bookingId);

    /**
     * Récupère toutes les commissions d'un revendeur avec pagination.
     */
    Page<ResellerCommissionEntry> findByResellerId(String resellerId, Pageable pageable);

    /**
     * Récupère les commissions d'un revendeur filtrées par statut.
     */
    Page<ResellerCommissionEntry> findByResellerIdAndStatus(
            String resellerId,
            ResellerCommissionStatus status,
            Pageable pageable
    );

    /**
     * Récupère les commissions créées dans une plage de dates pour un revendeur.
     */
    List<ResellerCommissionEntry> findByResellerIdAndCreatedAtBetween(
            String resellerId,
            Instant startDate,
            Instant endDate
    );

    /**
     * Calcule le total cumulé des commissions pour un revendeur selon leur statut (ex: AVAILABLE, PENDING).
     */
    @Query("""
        SELECT SUM(c.amount) 
        FROM ResellerCommissionEntry c 
        WHERE c.resellerId = :resellerId 
          AND c.status = :status
    """)
    Optional<BigDecimal> sumAmountByResellerIdAndStatus(
            @Param("resellerId") String resellerId,
            @Param("status") ResellerCommissionStatus status
    );
}