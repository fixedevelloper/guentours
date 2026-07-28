package com.guentours.search.web;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PropertySearchRequest(
        @NotBlank String city,
        @NotNull @FutureOrPresent LocalDate checkIn,
        @NotNull @FutureOrPresent LocalDate checkOut,
        @Min(1) Integer guests,
        Integer bedrooms,
        String propertyType,
        Boolean entirePlace,
        @NotBlank String currency
) {
    public com.guentours.provider.PropertySearchCriteria toCriteria() {
        return new com.guentours.provider.PropertySearchCriteria(
                city,
                checkIn,
                checkOut,
                guests == null ? 1 : guests,
                bedrooms,
                propertyType,
                entirePlace != null && entirePlace,
                currency == null || currency.isBlank() ? "XAF" : currency
        );
    }
}