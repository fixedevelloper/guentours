package com.guentours.reseller.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Représente une demande de retrait de solde du portefeuille d'un revendeur.
 */
@Entity
@Table(name = "reseller_withdrawals")
@Getter
public class ResellerWithdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "reseller_id", nullable = false, length = 36)
    private String resellerId;

    /** Montant demandé pour le retrait. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** Solde restant dans le portefeuille au moment de la création du retrait. */
    @Column(name = "remaining_wallet", nullable = false, precision = 12, scale = 2)
    private BigDecimal remainingWallet;

    @Column(nullable = false, length = 3)
    private String currency = "XAF";

    /** Moyen de paiement choisi (ex: 'ORANGE_MONEY', 'MTN_MOMO', 'BANK_TRANSFER'). */
    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;

    /** Détails du compte récepteur (ex: Numéro de téléphone Momo, IBAN/RIB). */
    @Column(name = "payment_details", columnDefinition = "TEXT")
    private String paymentDetails;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResellerWithdrawalStatus status = ResellerWithdrawalStatus.PENDING;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;

    protected ResellerWithdrawal() {}

    public ResellerWithdrawal(String resellerId,
                              BigDecimal amount,
                              BigDecimal remainingWallet,
                              String currency,
                              String paymentMethod,
                              String paymentDetails) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du retrait doit être strictement positif.");
        }
        this.resellerId = resellerId;
        this.amount = amount;
        this.remainingWallet = remainingWallet;
        this.currency = (currency != null) ? currency : "XAF";
        this.paymentMethod = paymentMethod;
        this.paymentDetails = paymentDetails;
        this.status = ResellerWithdrawalStatus.PENDING;
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
     * Passe la demande à l'état "En cours de traitement".
     */
    public void markProcessing() {
        this.status = ResellerWithdrawalStatus.PROCESSING;
    }

    /**
     * Valide le retrait après transfert effectif des fonds.
     */
    public void approve() {
        this.status = ResellerWithdrawalStatus.APPROVED;
        this.processedAt = Instant.now();
    }

    /**
     * Rejette la demande de retrait en précisant le motif.
     */
    public void reject(String reason) {
        this.status = ResellerWithdrawalStatus.REJECTED;
        this.rejectionReason = reason;
        this.processedAt = Instant.now();
    }
}