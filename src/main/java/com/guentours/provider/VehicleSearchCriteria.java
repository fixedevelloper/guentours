package com.guentours.provider;

import java.time.LocalDate;
import java.time.LocalTime;

public record VehicleSearchCriteria(
        String pickupCity,
        String dropoffCity,           // null/égal à pickupCity = restitution au même endroit
        LocalDate rentalStart,
        LocalTime pickupTime,
        LocalDate rentalEnd,
        LocalTime dropoffTime,
        String category,              // optionnel, filtre
        boolean withDriver,           // ⚠️ informatif uniquement pour l'instant, aucun impact prix/filtrage
        boolean driverAge25Plus,      // ⚠️ idem
        String currency
) {}