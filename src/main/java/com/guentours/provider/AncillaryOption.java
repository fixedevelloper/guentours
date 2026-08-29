package com.guentours.provider;

import com.guentours.shared.Money;

/**
 * One purchasable extra offered alongside a flight offer (extra baggage, a meal, a paid seat, or
 * GuenTours' own travel insurance line). {@code providerToken} is an opaque value the originating
 * adapter needs back, unmodified, to actually apply the selection when the booking is placed with
 * the provider - null for INSURANCE, which is never sent upstream. {@code paxRef} is the
 * provider's positional passenger reference (e.g. Travel Terminus's "T1"/"T2"), purely
 * informational; null for a booking-level option like INSURANCE. {@code seatLayout} is populated
 * only for {@code type == SEAT} and lets the frontend render an actual cabin grid instead of a
 * flat list - null for every other type.
 */
public record AncillaryOption(
        AncillaryType type,
        String segmentId,
        String code,
        String label,
        Money price,
        String paxRef,
        String providerToken,
        SeatLayout seatLayout
) {
}
