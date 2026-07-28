package com.guentours.search.domain;

import java.time.LocalDate;
import java.util.List;

public record HarmonizedPropertyOffer(
        String title,
        String propertyType,
        String city,
        String country,
        int bedrooms,
        int maxGuests,
        boolean entirePlace,
        LocalDate checkIn,
        LocalDate checkOut,
        String bestOfferId,
        List<ProviderQuote> quotes
) {}