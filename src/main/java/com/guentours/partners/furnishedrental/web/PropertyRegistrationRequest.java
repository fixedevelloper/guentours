package com.guentours.partners.furnishedrental.web;

import com.guentours.partners.furnishedrental.domain.PropertyType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record PropertyRegistrationRequest(
        @NotBlank String title,
        @NotNull PropertyType propertyType,
        @NotBlank String address,
        @NotBlank String city,
        @NotBlank String country,
        @NotNull @Min(0) Integer bedrooms,
        @NotNull @Min(0) Integer bathrooms,
        @NotNull @Min(1) Integer maxGuests,
        List<String> amenities,
        @NotNull @DecimalMin("0.0") BigDecimal pricePerNight,
        @NotBlank String currency,
        @NotNull @Min(1) Integer minStayNights,
        String description
) {}