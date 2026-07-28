package com.guentours.provider;

import java.util.List;

public record VehicleBookingRequest(
        VehicleOffer offer,
        List<PassengerInfo> drivers,
        String contactEmail
) {}