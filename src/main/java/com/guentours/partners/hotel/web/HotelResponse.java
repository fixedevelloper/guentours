package com.guentours.partners.hotel.web;

import com.guentours.partners.hotel.domain.Hotel;
import com.guentours.partners.hotel.domain.ListingStatus;

import java.util.List;

public record HotelResponse(
        String id,
        String name,
        String city,
        String country,
        Integer starRating,
        ListingStatus status,
        String coverImageUrl,
        List<HotelImageResponse> images
) {
    public static HotelResponse from(Hotel h) {
        return new HotelResponse(h.getId(), h.getName(), h.getCity(), h.getCountry(), h.getStarRating(), h.getStatus(),
                h.getCoverImageUrl(), h.getImages().stream().map(HotelImageResponse::from).toList());
    }
}
