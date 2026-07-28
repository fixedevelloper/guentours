package com.guentours.partners.hotel.web;

import com.guentours.partners.hotel.domain.ListingStatus;
import com.guentours.partners.hotel.domain.RoomType;
import java.math.BigDecimal;
import java.util.List;

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
        String coverImageUrl,
        List<RoomImageResponse> images,
        ListingStatus status
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
                r.getCoverImageUrl(),
                r.getImages().stream().map(RoomImageResponse::from).toList(),
                r.getStatus()
        );
    }
}