package com.guentours.reseller.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Entrée de registre (Ledger) créditée à un revendeur lorsqu'une réservation
 * effectuée avec son code promo passe à un état payé/confirmé.
 */
@Entity
@Table(
        name = "reseller_commission_entries",
        indexes = {
                @Index(name = "idx_rce_reseller_id", columnList = "reseller_id"),
                @Index(name = "idx_rce_booking_id", columnList = "booking_id", unique = true),
                @Index(name = "idx_rce_status", columnList = "status")
        }
)
@Getter
public class ResellerCommissionEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "reseller_id", nullable = false, length = 36)
    private String resellerId;

    @Column(name = "booking_id", nullable = false, unique = true, length = 36)
    private String bookingId;

    /** Montant total de la réservation au moment du paiement. */
    @Column(name = "booking_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal bookingAmount;

    /** Taux de commission appliqué au moment de la réservation (ex: 0.0500 pour 5%). */
    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionRate;

    /** Montant final de la commission attribuée au revendeur. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "XAF";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResellerCommissionStatus status = ResellerCommissionStatus.PENDING;

    /** Identifiant de la demande de virement / versement (populé lors du paiement). */
    @Column(name = "payout_id", length = 36)
    private String payoutId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected ResellerCommissionEntry() {
        // Constructeur requis par JPA
    }

    /**
     * Constructeur principal avec calcul automatique de la commission et arrondi bancaire standard.
     */
    public ResellerCommissionEntry(String resellerId,
                                   String bookingId,
                                   BigDecimal bookingAmount,
                                   BigDecimal commissionRate,
                                   String currency) {
        this.resellerId = resellerId;
        this.bookingId = bookingId;
        this.bookingAmount = bookingAmount;
        this.commissionRate = commissionRate;
        this.amount = bookingAmount.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);
        this.currency = (currency != null && !currency.isBlank()) ? currency : "XAF";
        this.status = ResellerCommissionStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Constructeur alternatif permettant de forcer un montant de commission spécifique.
     */
    public ResellerCommissionEntry(String resellerId,
                                   String bookingId,
                                   BigDecimal bookingAmount,
                                   BigDecimal commissionRate,
                                   BigDecimal amount,
                                   String currency) {
        this.resellerId = resellerId;
        this.bookingId = bookingId;
        this.bookingAmount = bookingAmount;
        this.commissionRate = commissionRate;
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = (currency != null && !currency.isBlank()) ? currency : "XAF";
        this.status = ResellerCommissionStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // =========================================================================
    // Transitions d'état & Logique Métier
    // =========================================================================

    /**
     * Valide la commission et la rend disponible/éligible pour un retrait.
     */
    public void approve() {
        if (this.status != ResellerCommissionStatus.PENDING) {
            throw new IllegalStateException("Seule une commission à l'état PENDING peut être approuvée.");
        }
        this.status = ResellerCommissionStatus.AVAILABLE;
    }

    /**
     * Annule la commission (ex: annulation ou remboursement du billet/hôtel).
     */
    public void cancel() {
        if (this.status == ResellerCommissionStatus.PAID) {
            throw new IllegalStateException("Impossible d'annuler une commission déjà versée au revendeur.");
        }
        this.status = ResellerCommissionStatus.CANCELLED;
    }

    /**
     * Marque la commission comme payée au revendeur.
     */
    public void markPaid() {
        if (this.status != ResellerCommissionStatus.AVAILABLE) {
            throw new IllegalStateException("Seule une commission AVAILABLE peut être marquée comme payée.");
        }
        this.status = ResellerCommissionStatus.PAID;
    }

    /**
     * Marque la commission comme payée au revendeur en l'associant à un lot de versement.
     */
    public void markPaid(String payoutId) {
        markPaid();
        this.payoutId = payoutId;
    }
}