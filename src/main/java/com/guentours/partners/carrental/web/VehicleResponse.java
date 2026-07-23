package com.guentours.partners.carrental.web;

import com.guentours.partners.carrental.domain.ListingStatus;
import com.guentours.partners.carrental.domain.Vehicle;
import com.guentours.partners.carrental.domain.VehicleCategory;

import java.math.BigDecimal;

public record VehicleResponse(
        String id,
        String brand,
        String model,
        Integer year,
        VehicleCategory category,
        BigDecimal pricePerDay,
        Integer unitsCount,
        ListingStatus status
) {
    public static VehicleResponse from(Vehicle v) {
        return new VehicleResponse(v.getId(), v.getBrand(), v.getModel(), v.getYear(),
                v.getCategory(), v.getPricePerDay(), v.getUnitsCount(), v.getStatus());
    }
}
