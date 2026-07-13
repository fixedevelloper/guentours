package com.guentours.provider;

import java.time.LocalDate;

public record HotelSearchCriteria(
        String cityCode,
        LocalDate checkIn,
        LocalDate checkOut,
        int adults,
        int rooms
) {
}
