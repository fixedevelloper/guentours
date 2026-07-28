package com.guentours.geo;

public record HotelCityAdminResponse(Long id, String cityName, String countryName, double latitude, double longitude) {

    static HotelCityAdminResponse from(HotelCity city) {
        return new HotelCityAdminResponse(city.getId(), city.getCityName(), city.getCountryName(),
                city.getLatitude(), city.getLongitude());
    }
}
