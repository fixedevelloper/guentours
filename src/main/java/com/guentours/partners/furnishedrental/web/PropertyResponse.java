package com.guentours.partners.furnishedrental.web;

import com.guentours.partners.furnishedrental.domain.ListingStatus;
import com.guentours.partners.furnishedrental.domain.Property;
import com.guentours.partners.furnishedrental.domain.PropertyType;

import java.math.BigDecimal;

public record PropertyResponse(
        String id,
        String partnerId,
        String title,
        PropertyType propertyType,
        String city,
        String country,
        Integer bedrooms,
        Integer bathrooms,
        Integer maxGuests,
        BigDecimal pricePerNight,
        Integer minStayNights,
        ListingStatus status
) {
    public static PropertyResponse from(Property property) {
        return new PropertyResponse(
                property.getId(),
                property.getPartnerId(),
                property.getTitle(),
                property.getPropertyType(),
                property.getCity(),
                property.getCountry(),
                property.getBedrooms(),
                property.getBathrooms(),
                property.getMaxGuests(),
                property.getPricePerNight(),
                property.getMinStayNights(),
                property.getStatus()
        );
    }
}