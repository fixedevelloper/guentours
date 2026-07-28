package com.guentours.partners.furnishedrental.web;

import com.guentours.partners.furnishedrental.domain.PropertyImage;

public record PropertyImageResponse(
        String id,
        String url,
        String caption,
        boolean isPrimary,
        Integer displayOrder
) {
    public static PropertyImageResponse from(PropertyImage image) {
        return new PropertyImageResponse(
                image.getId(),
                image.getUrl(),
                image.getCaption(),
                image.isPrimary(),
                image.getDisplayOrder()
        );
    }
}
