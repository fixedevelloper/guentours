package com.guentours.provider;

import com.guentours.shared.Money;

import java.time.LocalDate;

public record HotelOffer(
        ProviderType providerType,
        String providerOfferId,
        String hotelName,
        String cityCode,
        String roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        Money price,
        double rating
) {
    /** Key used to detect that two providers are quoting the same physical room. */
    public String harmonizationKey() {
        return "%s|%s|%s|%s|%s".formatted(hotelName, cityCode, roomType, checkIn, checkOut);
    }
}
