package com.guentours.provider;

import java.time.LocalDate;

public record PropertySearchCriteria(
        String city,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        Integer bedrooms,       // filtre "au moins N chambres", null = pas de filtre
        String propertyType,    // optionnel, filtre
        boolean entirePlace,    // ⚠️ informatif uniquement, aucun impact filtrage/prix (Property n'a pas ce concept)
        String currency
) {}