package com.guentours.provider;

import java.util.List;

public record PropertyBookingRequest(
        PropertyOffer offer,
        List<PassengerInfo> guests,
        String contactEmail
) {}