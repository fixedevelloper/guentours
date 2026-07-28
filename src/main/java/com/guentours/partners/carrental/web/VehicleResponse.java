package com.guentours.partners.carrental.web;

import com.guentours.partners.carrental.domain.ListingStatus;
import com.guentours.partners.carrental.domain.Transmission;
import com.guentours.partners.carrental.domain.Vehicle;
import com.guentours.partners.carrental.domain.VehicleCategory;

import java.math.BigDecimal;
import java.util.List;

public record VehicleResponse(
        String id,
        String partnerId,
        String brand,
        String model,
        Integer year,
        VehicleCategory category,
        Transmission transmission,
        Integer seats,
        Boolean airConditioning,
        BigDecimal pricePerDay,
        String currency,
        Integer unitsCount,
        List<String> pickupLocations,
        ListingStatus status,
        String coverImageUrl,
        List<VehicleImageResponse> images
) {
    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getPartnerId(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getCategory(),
                vehicle.getTransmission(),
                vehicle.getSeats(),
                vehicle.getAirConditioning(),
                vehicle.getPricePerDay(),
                vehicle.getCurrency(),
                vehicle.getUnitsCount(),
                vehicle.getPickupLocations(),
                vehicle.getStatus(),
                vehicle.getCoverImageUrl(),
                vehicle.getImages().stream().map(VehicleImageResponse::from).toList()
        );
    }
}
