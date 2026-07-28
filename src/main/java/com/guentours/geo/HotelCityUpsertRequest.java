package com.guentours.geo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HotelCityUpsertRequest(
        @NotBlank String cityName,
        @NotBlank String countryName,
        @NotNull Double latitude,
        @NotNull Double longitude
) {}
