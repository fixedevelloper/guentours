package com.guentours.partners.furnishedrental.web;

import com.guentours.partners.furnishedrental.domain.ListingStatus;
import com.guentours.partners.furnishedrental.domain.Property;
import com.guentours.partners.furnishedrental.domain.PropertyType;

import java.math.BigDecimal;

public record PropertyResponse(
        String id,
        String title,
        PropertyType propertyType,
        String city,
        String country,
        Integer bedrooms,
        Integer maxGuests,
        BigDecimal pricePerNight,
        ListingStatus status
) {
    public static PropertyResponse from(Property p) {
        return new PropertyResponse(
                p.getId(), p.getTitle(), p.getPropertyType(), p.getCity(), p.getCountry(),
                p.getBedrooms(), p.getMaxGuests(), p.getPricePerNight(), p.getStatus()
        );
    }
}
