package com.guentours.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Admin-configurable rule saying which payment gateway handles a given payment method for a given
 * country - e.g. Flutterwave for CARD in Cameroon, a different operator for MOBILE_MONEY.
 * {@link #countryCode} {@code null} means the rule is the default for every country not covered by
 * a more specific rule for that same {@link #paymentMethod}. Looked up by
 * {@code PaymentProviderRoutingService} before every charge; deactivating a rule (rather than
 * deleting it) rejects new charges for that country/method combination without losing the
 * configuration.
 */
@Entity
@Table(name = "payment_provider_routes")
public class PaymentProviderRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "provider_name", nullable = false)
    private String providerName;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected PaymentProviderRoute() {
        // JPA
    }

    public PaymentProviderRoute(String countryCode, PaymentMethod paymentMethod, String providerName) {
        this.countryCode = countryCode;
        this.paymentMethod = paymentMethod;
        this.providerName = providerName;
    }

    public String getId() {
        return id;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
