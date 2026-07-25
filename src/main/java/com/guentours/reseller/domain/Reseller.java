package com.guentours.reseller.domain;

import com.guentours.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "resellers")
@Getter
public class Reseller {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", unique = true, length = 36)
    private String userId;

    /**
     * Association JPA vers l'entité User.
     * Lombok générera automatiquement la méthode getUser().
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "contact_name", nullable = false)
    private String contactName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(name = "promo_code", nullable = false, unique = true, length = 20)
    private String promoCode;

    @Column(name = "registration_number", nullable = false, unique = true, length = 100)
    private String registrationNumber;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    /** Taux de commission (ex: 0.0500 = 5%). */
    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionRate = new BigDecimal("0.0500");

    /** Solde actuel du portefeuille revendeur. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal wallet = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResellerStatus status = ResellerStatus.PENDING_REVIEW;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected Reseller() {}

    /** Constructeur complet pour la création d'un revendeur. */
    public Reseller(String userId,
                    String companyName,
                    String contactName,
                    String email,
                    String phone,
                    String registrationNumber,
                    String city,
                    String country,
                    String promoCode,
                    BigDecimal commissionRate,
                    String logoUrl) {
        this.userId = userId;
        this.companyName = companyName;
        this.contactName = contactName;
        this.email = email;
        this.phone = phone;
        this.registrationNumber = registrationNumber;
        this.city = city;
        this.country = country;
        this.promoCode = promoCode;
        this.commissionRate = (commissionRate != null) ? commissionRate : new BigDecimal("0.0500");
        this.logoUrl = logoUrl;
        this.wallet = BigDecimal.ZERO;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // =========================================================================
    // Méthodes de domaine & Logique Métier
    // =========================================================================

    public void approve() {
        this.status = ResellerStatus.APPROVED;
        this.reviewedAt = Instant.now();
    }

    public void reject() {
        this.status = ResellerStatus.REJECTED;
        this.reviewedAt = Instant.now();
    }

    public void suspend() {
        this.status = ResellerStatus.SUSPENDED;
    }

    public void reactivate() {
        this.status = ResellerStatus.APPROVED;
    }

    public void setCommissionRate(BigDecimal commissionRate) {
        if (commissionRate == null || commissionRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le taux de commission doit être supérieur ou égal à zéro.");
        }
        this.commissionRate = commissionRate;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Crédite le solde du portefeuille.
     */
    public void creditWallet(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant à créditer doit être strictement positif.");
        }
        this.wallet = this.wallet.add(amount);
    }

    /**
     * Débite le solde du portefeuille lors d'un retrait.
     */
    public void debitWallet(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant à débiter doit être strictement positif.");
        }
        if (this.wallet.compareTo(amount) < 0) {
            throw new IllegalStateException("Solde insuffisant dans le portefeuille.");
        }
        this.wallet = this.wallet.subtract(amount);
    }
}