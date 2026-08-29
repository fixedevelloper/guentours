package com.guentours.booking.web;

import com.guentours.provider.AncillaryOption;
import com.guentours.provider.AncillaryType;
import com.guentours.provider.SeatLayout;
import com.guentours.shared.Money;

/**
 * {@code id} is the {@code OfferCache} key the frontend must echo back (in a traveler's {@code
 * selectedAncillaryIds}) to actually pick this extra at checkout - never the raw provider token,
 * which stays server-side. {@code seatLayout} is set only for {@code type == SEAT}, letting the
 * frontend render an actual cabin grid instead of a flat list.
 */
public record AncillaryOptionResponse(
        String id,
        AncillaryType type,
        String segmentId,
        String code,
        String label,
        Money price,
        String paxRef,
        SeatLayout seatLayout
) {
    public static AncillaryOptionResponse from(String id, AncillaryOption option) {
        return new AncillaryOptionResponse(id, option.type(), option.segmentId(), option.code(), option.label(),
                option.price(), option.paxRef(), option.seatLayout());
    }
}
