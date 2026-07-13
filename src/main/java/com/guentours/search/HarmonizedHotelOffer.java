package com.guentours.search;

import java.time.LocalDate;
import java.util.List;

/**
 * The result of merging every provider's quote for the same room product
 * (same hotel, room type and stay dates) into one entry, ranked by price.
 */
public record HarmonizedHotelOffer(
        String hotelName,
        String cityCode,
        String roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        double rating,
        String bestOfferId,
        List<ProviderQuote> quotes
) {
}
