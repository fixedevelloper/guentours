package com.guentours.booking.domain;

import com.guentours.provider.AncillaryType;
import com.guentours.shared.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;

/**
 * One extra (baggage/meal/seat/insurance) attached to a booking at checkout - a snapshot of what
 * was quoted and picked, kept for pricing/audit even after the originating {@code OfferCache}
 * entry has expired. {@code travelerIndex} is the traveler's position in {@code Booking#travelers}
 * (null for a booking-level extra like INSURANCE); {@code providerToken} is the opaque value the
 * originating provider adapter needs back unmodified to apply the selection at hold time (null
 * for INSURANCE, which is never sent upstream).
 */
@Embeddable
public class BookingExtra {

    @Enumerated(EnumType.STRING)
    @Column(name = "extra_type", nullable = false, length = 20)
    private AncillaryType type;

    @Column(name = "traveler_index")
    private Integer travelerIndex;

    @Column(name = "segment_id")
    private String segmentId;

    private String code;

    private String label;

    @Embedded
    private Money price;

    @Lob
    @Column(name = "provider_token")
    private String providerToken;

    protected BookingExtra() {
        // JPA
    }

    public BookingExtra(AncillaryType type, Integer travelerIndex, String segmentId, String code, String label,
                         Money price, String providerToken) {
        this.type = type;
        this.travelerIndex = travelerIndex;
        this.segmentId = segmentId;
        this.code = code;
        this.label = label;
        this.price = price;
        this.providerToken = providerToken;
    }

    public AncillaryType getType() {
        return type;
    }

    public Integer getTravelerIndex() {
        return travelerIndex;
    }

    public String getSegmentId() {
        return segmentId;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public Money getPrice() {
        return price;
    }

    public String getProviderToken() {
        return providerToken;
    }
}
