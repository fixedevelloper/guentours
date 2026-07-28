package com.guentours.partners.hotel.web;

import com.guentours.partners.hotel.domain.RoomTypeImage;

public record RoomImageResponse(
        String id,
        String url,
        String caption,
        boolean isPrimary,
        Integer displayOrder
) {
    public static RoomImageResponse from(RoomTypeImage image) {
        return new RoomImageResponse(
                image.getId(),
                image.getUrl(),
                image.getCaption(),
                image.isPrimary(),
                image.getDisplayOrder()
        );
    }
}
