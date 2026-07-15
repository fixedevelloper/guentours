package com.guentours.commission;

import com.guentours.booking.OfferType;
import com.guentours.provider.ProviderType;
import com.guentours.shared.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One fixed booking fee earned on a single booking, recorded at checkout time. The amount is
 * never deducted from the provider's price - it is charged to the customer on top of it and
 * only ever recorded here, so this ledger is the sole record of GuenTours' own commission revenue.
 */
@Entity
@Table(name = "commission_wallet_entries")
public class CommissionWalletEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "booking_id", nullable = false)
    private String bookingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false)
    private ProviderType providerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_type", nullable = false)
    private OfferType offerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "commission_type", nullable = false)
    private CommissionType commissionType = CommissionType.BOOKING_FEE;

    @Embedded
    private Money amount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected CommissionWalletEntry() {
        // JPA
    }

    public CommissionWalletEntry(String bookingId, ProviderType providerType, OfferType offerType,
                                 CommissionType commissionType, Money amount) {
        this.bookingId = bookingId;
        this.providerType = providerType;
        this.offerType = offerType;
        this.commissionType = commissionType;
        this.amount = amount;
    }

    public String getId() {
        return id;
    }

    public String getBookingId() {
        return bookingId;
    }

    public ProviderType getProviderType() {
        return providerType;
    }

    public OfferType getOfferType() {
        return offerType;
    }

    public CommissionType getCommissionType() {
        return commissionType;
    }

    public Money getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
