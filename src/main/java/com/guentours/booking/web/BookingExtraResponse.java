package com.guentours.booking.web;

import com.guentours.booking.domain.BookingExtra;
import com.guentours.provider.AncillaryType;
import com.guentours.shared.Money;

public record BookingExtraResponse(AncillaryType type, Integer travelerIndex, String segmentId, String code,
                                    String label, Money price) {
    public static BookingExtraResponse from(BookingExtra extra) {
        return new BookingExtraResponse(extra.getType(), extra.getTravelerIndex(), extra.getSegmentId(),
                extra.getCode(), extra.getLabel(), extra.getPrice());
    }
}
