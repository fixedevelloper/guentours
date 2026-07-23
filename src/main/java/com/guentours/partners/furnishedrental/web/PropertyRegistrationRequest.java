package com.guentours.partners.furnishedrental.web;

import com.guentours.partners.furnishedrental.domain.PropertyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record PropertyRegistrationRequest(
        @NotBlank String title,
        @NotNull PropertyType propertyType,
        @NotBlank String address,
        @NotBlank String city,
        @NotBlank String country,
        @NotNull Integer bedrooms,
        @NotNull Integer bathrooms,
        @NotNull Integer maxGuests,
        List<String> amenities,
        @NotNull BigDecimal pricePerNight,
        @NotBlank String currency,
        @NotNull Integer minStayNights,
        String description
) {}
