package com.guentours.partners.furnishedrental.web;

import com.guentours.partners.furnishedrental.domain.ListingStatus;
import com.guentours.partners.furnishedrental.domain.Property;
import com.guentours.partners.furnishedrental.domain.PropertyType;

import java.math.BigDecimal;
import java.util.List;

public record PropertyResponse(
        String id,
        String partnerId,
        String title,
        PropertyType propertyType,
        String address,
        String city,
        String country,
        Integer bedrooms,
        Integer bathrooms,
        Integer maxGuests,
        List<String> amenities,
        BigDecimal pricePerNight,
        String currency,
        Integer minStayNights,
        String description,
        ListingStatus status,
        String coverImageUrl,
        List<PropertyImageResponse> images
) {
    public static PropertyResponse from(Property property) {
        return new PropertyResponse(
                property.getId(),
                property.getPartnerId(),
                property.getTitle(),
                property.getPropertyType(),
                property.getAddress(),
                property.getCity(),
                property.getCountry(),
                property.getBedrooms(),
                property.getBathrooms(),
                property.getMaxGuests(),
                property.getAmenities(),
                property.getPricePerNight(),
                property.getCurrency(),
                property.getMinStayNights(),
                property.getDescription(),
                property.getStatus(),
                property.getCoverImageUrl(),
                property.getImages().stream().map(PropertyImageResponse::from).toList()
        );
    }
}
