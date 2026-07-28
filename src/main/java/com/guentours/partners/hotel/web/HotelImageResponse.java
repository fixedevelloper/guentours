package com.guentours.partners.hotel.web;

import com.guentours.partners.hotel.domain.HotelImage;

public record HotelImageResponse(
        String id,
        String url,
        String caption,
        boolean isPrimary,
        Integer displayOrder
) {
    public static HotelImageResponse from(HotelImage image) {
        return new HotelImageResponse(
                image.getId(),
                image.getUrl(),
                image.getCaption(),
                image.isPrimary(),
                image.getDisplayOrder()
        );
    }
}
