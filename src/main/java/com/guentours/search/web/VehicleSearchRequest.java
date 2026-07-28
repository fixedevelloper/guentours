package com.guentours.search.web;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public record VehicleSearchRequest(
        @NotBlank String pickupCity,
        String dropoffCity,
        @NotNull @FutureOrPresent
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate rentalStart,
        @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime pickupTime,
        @NotNull @FutureOrPresent
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate rentalEnd,
        @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime dropoffTime,
        String category,
        Boolean withDriver,
        Boolean driverAge25Plus,
        @NotBlank String currency
) {
    /**
     * Convertit ce DTO web (avec sa validation Bean Validation) vers le critère de domaine
     * consommé par VehicleSearchService/TravelProviderClient, en appliquant les mêmes
     * valeurs par défaut que l'ancien endpoint @RequestParam :
     * withDriver=false, driverAge25Plus=true, currency=XAF si absents.
     */
    public com.guentours.provider.VehicleSearchCriteria toCriteria() {
        return new com.guentours.provider.VehicleSearchCriteria(
                pickupCity,
                dropoffCity,
                rentalStart,
                pickupTime,
                rentalEnd,
                dropoffTime,
                category,
                withDriver != null && withDriver,
                driverAge25Plus == null || driverAge25Plus,
                currency == null || currency.isBlank() ? "XAF" : currency
        );
    }
}