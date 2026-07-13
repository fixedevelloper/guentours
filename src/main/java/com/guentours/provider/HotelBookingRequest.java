package com.guentours.provider;

import java.util.List;

public record HotelBookingRequest(HotelOffer offer, List<PassengerInfo> guests, String contactEmail) {
}
