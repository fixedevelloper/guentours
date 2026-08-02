package com.guentours.destination;

import java.time.Instant;

public record FeaturedDestinationAdminResponse(String id, String cityName, String countryName, String destinationCode,
                                               String imageUrl, int displayOrder, boolean active, Instant createdAt) {

    public static FeaturedDestinationAdminResponse from(FeaturedDestination destination) {
        return new FeaturedDestinationAdminResponse(destination.getId(), destination.getCityName(),
                destination.getCountryName(), destination.getDestinationCode(), destination.getImageUrl(),
                destination.getDisplayOrder(), destination.isActive(), destination.getCreatedAt());
    }
}
