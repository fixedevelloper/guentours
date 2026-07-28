package com.guentours.partners.carrental.web;

import com.guentours.partners.carrental.domain.VehicleImage;

public record VehicleImageResponse(
        String id,
        String url,
        String caption,
        boolean isPrimary,
        Integer displayOrder
) {
    public static VehicleImageResponse from(VehicleImage image) {
        return new VehicleImageResponse(
                image.getId(),
                image.getUrl(),
                image.getCaption(),
                image.isPrimary(),
                image.getDisplayOrder()
        );
    }
}
