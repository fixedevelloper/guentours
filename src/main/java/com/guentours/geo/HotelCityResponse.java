package com.guentours.geo;

public record HotelCityResponse(String cityName, String countryName, double latitude, double longitude) {

    static HotelCityResponse from(HotelCity city) {
        return new HotelCityResponse(city.getCityName(), city.getCountryName(), city.getLatitude(),
                city.getLongitude());
    }
}
