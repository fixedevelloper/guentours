package com.guentours.destination;

import jakarta.validation.constraints.NotBlank;

public record FeaturedDestinationUpsertRequest(
        @NotBlank String cityName,
        @NotBlank String countryName,
        String destinationCode,
        String imageUrl,
        int displayOrder,
        boolean active
) {}
