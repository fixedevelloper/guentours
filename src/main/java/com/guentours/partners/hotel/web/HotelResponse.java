package com.guentours.partners.hotel.web;

import com.guentours.partners.hotel.domain.Hotel;
import com.guentours.partners.hotel.domain.ListingStatus;

public record HotelResponse(
        String id,
        String name,
        String city,
        String country,
        Integer starRating,
        ListingStatus status
) {
    public static HotelResponse from(Hotel h) {
        return new HotelResponse(h.getId(), h.getName(), h.getCity(), h.getCountry(), h.getStarRating(), h.getStatus());
    }
}
