package com.guentours.geo;

public record AirportResponse(String airportCode, String airportName, String city, String country) {

    static AirportResponse from(Airport airport) {
        return new AirportResponse(airport.getAirportCode(), airport.getAirportName(), airport.getCity(),
                airport.getCountry());
    }
}
