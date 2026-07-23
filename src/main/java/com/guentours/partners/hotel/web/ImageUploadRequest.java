package com.guentours.partners.hotel.web;

public record ImageUploadRequest(
        String url,
        String caption,
        Integer displayOrder,
        boolean isPrimary
) {}