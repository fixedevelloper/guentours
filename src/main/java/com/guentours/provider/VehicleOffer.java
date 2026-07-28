package com.guentours.provider;

import com.guentours.shared.Money;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

public record VehicleOffer(
        ProviderType providerType,
        String providerOfferId,
        String brand,
        String model,
        String category,
        String transmission,
        int seats,
        boolean airConditioning,
        String pickupCity,
        String dropoffCity,
        LocalDate rentalStart,
        LocalTime pickupTime,
        LocalDate rentalEnd,
        LocalTime dropoffTime,
        boolean withDriver,
        boolean driverAge25Plus,
        Money pricePerDay,
        Money totalPrice,
        Map<String, String> providerContext
) {
    public VehicleOffer {
        providerContext = providerContext == null ? Map.of() : Map.copyOf(providerContext);
    }

    public String harmonizationKey() {
        return "%s|%s|%s|%s|%s".formatted(brand, model, pickupCity, rentalStart, rentalEnd);
    }

    public String context(String key) {
        return providerContext.get(key);
    }
}