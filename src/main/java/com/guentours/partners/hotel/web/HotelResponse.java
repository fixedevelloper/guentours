package com.guentours.partners.hotel.web;

import com.guentours.partners.hotel.domain.Hotel;
import com.guentours.partners.hotel.domain.ListingStatus;

import java.time.LocalTime;
import java.util.List;

public record HotelResponse(
        String id,
        String partnerId,
        String name,
        String address,
        String city,
        String country,
        Integer starRating,
        String description,
        List<String> amenities,
        LocalTime checkInTime,
        LocalTime checkOutTime,
        ListingStatus status,
        String coverImageUrl,
        List<HotelImageResponse> images
) {
    public static HotelResponse from(Hotel h) {
        return new HotelResponse(
                h.getId(),
                h.getPartnerId(),
                h.getName(),
                h.getAddress(),
                h.getCity(),
                h.getCountry(),
                h.getStarRating(),
                h.getDescription(),
                h.getAmenities(),
                h.getCheckInTime(),
                h.getCheckOutTime(),
                h.getStatus(),
                h.getCoverImageUrl(),
                h.getImages().stream().map(HotelImageResponse::from).toList()
        );
    }
}
