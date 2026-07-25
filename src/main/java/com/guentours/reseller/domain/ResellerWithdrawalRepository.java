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
public interface ResellerWithdrawalRepository extends JpaRepository<ResellerWithdrawal, String> {

    /**
     * Récupère l'historique des demandes de retrait d'un revendeur avec pagination.
     */
    Page<ResellerWithdrawal> findByResellerId(String resellerId, Pageable pageable);

    /**
     * Récupère les demandes de retrait d'un revendeur filtrées par statut.
     */
    Page<ResellerWithdrawal> findByResellerIdAndStatus(
            String resellerId,
            ResellerWithdrawalStatus status,
            Pageable pageable
    );

    /**
     * Récupère toutes les demandes de retrait selon un statut (ex: PENDING pour la file de validation admin).
     */
    Page<ResellerWithdrawal> findByStatus(ResellerWithdrawalStatus status, Pageable pageable);

    /**
     * Vérifie si le revendeur a déjà une demande de retrait en cours (PENDING/PROCESSING).
     * Utile pour bloquer les demandes simultanées multiples.
     */
    boolean existsByResellerIdAndStatus(String resellerId, ResellerWithdrawalStatus status);

    /**
     * Récupère les demandes de retrait créées dans une période donnée pour un revendeur.
     */
    List<ResellerWithdrawal> findByResellerIdAndCreatedAtBetween(
            String resellerId,
            Instant startDate,
            Instant endDate
    );

    /**
     * Calcule la somme totale des montants retirés ou en cours de retrait pour un revendeur.
     */
    @Query("""
        SELECT SUM(w.amount) 
        FROM ResellerWithdrawal w 
        WHERE w.resellerId = :resellerId 
          AND w.status = :status
    """)
    Optional<BigDecimal> sumAmountByResellerIdAndStatus(
            @Param("resellerId") String resellerId,
            @Param("status") ResellerWithdrawalStatus status
    );
}