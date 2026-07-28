package com.guentours.storage.web;

import com.guentours.storage.domain.UserImage;

import java.time.Instant;

public record UserImageResponse(
        String id,
        String url,
        String name,
        Long sizeBytes,
        Instant createdAt
) {
    public static UserImageResponse from(UserImage image) {
        return new UserImageResponse(
                image.getId(),
                image.getUrl(),
                image.getOriginalFilename(),
                image.getSizeBytes(),
                image.getCreatedAt()
        );
    }
}