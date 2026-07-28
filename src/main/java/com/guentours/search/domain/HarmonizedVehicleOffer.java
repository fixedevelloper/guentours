package com.guentours.search.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record HarmonizedVehicleOffer(
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
        String bestOfferId,
        List<ProviderQuote> quotes
) {}