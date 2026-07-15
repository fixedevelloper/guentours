package com.guentours.booking;

import com.guentours.provider.PassengerType;

public record BookingTravelerResponse(String fullName, PassengerType type, String seatNumber) {
    public static BookingTravelerResponse from(BookedTraveler traveler) {
        return new BookingTravelerResponse(traveler.getFullName(), traveler.getType(), traveler.getSeatNumber());
    }
}
