package com.guentours.provider;

import com.guentours.shared.Money;

import java.time.LocalDate;
import java.util.Map;

/**
 * Canonical hotel offer shape every provider adapter maps its vendor-specific response into.
 *
 * <p>{@code providerContext} is an opaque, provider-private bag of extra identifiers an adapter
 * captured at search time and needs back at availability/booking time (e.g. Travelport's property
 * chain/property codes). It is excluded from {@link #harmonizationKey()} so it never affects
 * cross-provider merging; only the originating adapter reads its own keys.
 */
public record HotelOffer(
        ProviderType providerType,
        String providerOfferId,
        String hotelName,
        String cityCode,
        String roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        Money price,
        double rating,
        Map<String, String> providerContext
) {

    /** Convenience constructor for offers without any provider-specific context. */
    public HotelOffer(ProviderType providerType, String providerOfferId, String hotelName, String cityCode,
                      String roomType, LocalDate checkIn, LocalDate checkOut, Money price, double rating) {
        this(providerType, providerOfferId, hotelName, cityCode, roomType, checkIn, checkOut, price, rating, Map.of());
    }

    public HotelOffer {
        providerContext = providerContext == null ? Map.of() : Map.copyOf(providerContext);
    }

    /** Key used to detect that two providers are quoting the same physical room. */
    public String harmonizationKey() {
        return "%s|%s|%s|%s|%s".formatted(hotelName, cityCode, roomType, checkIn, checkOut);
    }

    /** Reads a provider-specific context value captured at search time, or {@code null} if absent. */
    public String context(String key) {
        return providerContext.get(key);
    }
}
