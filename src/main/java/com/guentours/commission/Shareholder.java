package com.guentours.commission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A shareholder entitled to a fixed percentage of every commission GuenTours earns, independent of
 * any user/reseller/partner account - this is a company equity split, not tied to who made the sale.
 * Deactivating one (rather than deleting it) keeps it out of future splits while past
 * {@link ShareholderCommissionEntry} rows - which snapshot the name and percentage applied - stay
 * meaningful.
 */
@Entity
@Table(name = "shareholders")
public class Shareholder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Shareholder() {
        // JPA
    }

    public Shareholder(String name, BigDecimal percentage) {
        this.name = name;
        this.percentage = percentage;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
