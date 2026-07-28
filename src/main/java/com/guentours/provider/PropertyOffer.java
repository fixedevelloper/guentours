package com.guentours.provider;

import com.guentours.shared.Money;
import java.time.LocalDate;
import java.util.Map;

public record PropertyOffer(
        ProviderType providerType,
        String providerOfferId,
        String title,
        String propertyType,
        String city,
        String country,
        int bedrooms,
        int maxGuests,
        boolean entirePlace, // ⚠️ reporté depuis la recherche, purement informatif
        LocalDate checkIn,
        LocalDate checkOut,
        Money pricePerNight,
        Money totalPrice,
        Map<String, String> providerContext
) {
    public PropertyOffer {
        providerContext = providerContext == null ? Map.of() : Map.copyOf(providerContext);
    }

    public String harmonizationKey() {
        return "%s|%s|%s|%s".formatted(title, city, checkIn, checkOut);
    }

    public String context(String key) {
        return providerContext.get(key);
    }
}