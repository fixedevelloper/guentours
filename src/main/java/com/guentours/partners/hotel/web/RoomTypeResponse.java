package com.guentours.partners.hotel.web;

import com.guentours.partners.hotel.domain.RoomType;
import java.math.BigDecimal;

public record RoomTypeResponse(
        String id,
        String name,
        Integer maxAdults,
        Integer maxChildren,
        String bedType,
        Double sizeSqm,
        BigDecimal basePrice,
        String currency,
        Integer totalRooms,
        String coverImageUrl
) {
    public static RoomTypeResponse from(RoomType r) {
        return new RoomTypeResponse(
                r.getId(),
                r.getName(),
                r.getMaxAdults(),
                r.getMaxChildren(),
                r.getBedType(),
                r.getSizeSqm(),
                r.getBasePrice(),
                r.getCurrency(),
                r.getTotalRooms(),
                r.getCoverImageUrl()
        );
    }
}