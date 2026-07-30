package com.guentours.commission;

import com.guentours.shared.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One shareholder's cut of a single {@link CommissionWalletEntry}, created automatically the moment
 * that entry is recorded (i.e. when a booking's commission is actually earned, never at search
 * time). Snapshots the shareholder's name and percentage as they were at that moment, so a later
 * change to {@link Shareholder#getPercentage()} - or the shareholder being deactivated altogether -
 * never rewrites the historical record.
 */
@Entity
@Table(name = "shareholder_commission_entries")
public class ShareholderCommissionEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "commission_wallet_entry_id", nullable = false)
    private String commissionWalletEntryId;

    @Column(name = "shareholder_id", nullable = false)
    private String shareholderId;

    @Column(name = "shareholder_name", nullable = false)
    private String shareholderName;

    @Column(name = "percentage_applied", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentageApplied;

    @Embedded
    private Money amount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ShareholderCommissionEntry() {
        // JPA
    }

    public ShareholderCommissionEntry(String commissionWalletEntryId, String shareholderId, String shareholderName,
                                      BigDecimal percentageApplied, Money amount) {
        this.commissionWalletEntryId = commissionWalletEntryId;
        this.shareholderId = shareholderId;
        this.shareholderName = shareholderName;
        this.percentageApplied = percentageApplied;
        this.amount = amount;
    }

    public String getId() {
        return id;
    }

    public String getCommissionWalletEntryId() {
        return commissionWalletEntryId;
    }

    public String getShareholderId() {
        return shareholderId;
    }

    public String getShareholderName() {
        return shareholderName;
    }

    public BigDecimal getPercentageApplied() {
        return percentageApplied;
    }

    public Money getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
