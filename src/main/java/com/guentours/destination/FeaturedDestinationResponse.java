package com.guentours.destination;

/** Public shape: only what the homepage card needs to render and deep-link into a flight search. */
public record FeaturedDestinationResponse(String cityName, String countryName, String destinationCode, String imageUrl) {

    public static FeaturedDestinationResponse from(FeaturedDestination destination) {
        return new FeaturedDestinationResponse(destination.getCityName(), destination.getCountryName(),
                destination.getDestinationCode(), destination.getImageUrl());
    }
}
