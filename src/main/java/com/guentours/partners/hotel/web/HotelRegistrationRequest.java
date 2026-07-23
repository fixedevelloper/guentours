package com.guentours.partners.hotel.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.List;

public record HotelRegistrationRequest(
        @NotBlank String name,
        @NotBlank String address,
        @NotBlank String city,
        @NotBlank String country,
        @NotNull Integer starRating,
        String description,
        @Size(max = 1024, message = "L'URL de l'image de couverture ne doit pas dépasser 1024 caractères")
        String coverImageUrl,
        List<String> amenities,
        LocalTime checkInTime,
        LocalTime checkOutTime
) {}