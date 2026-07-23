package com.guentours.partners.hotel.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RoomTypeRequest(
        @NotBlank String name,
        @NotNull Integer maxAdults,
        @NotNull Integer maxChildren,
        String bedType,
        Double sizeSqm,
        @NotNull BigDecimal basePrice,
        @NotBlank String currency,
        @NotNull Integer totalRooms,
        @Size(max = 1024, message = "L'URL de l'image de couverture ne doit pas dépasser 1024 caractères")
        String coverImageUrl
) {}