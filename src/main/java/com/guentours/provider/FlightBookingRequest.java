package com.guentours.provider;

import java.util.List;

public record FlightBookingRequest(FlightOffer offer, List<PassengerInfo> passengers, String contactEmail) {
}
