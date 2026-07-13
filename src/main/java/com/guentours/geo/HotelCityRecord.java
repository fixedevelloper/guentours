package com.guentours.geo;

/** One city as returned by a {@link HotelCityDataSource}, before it's persisted. */
public record HotelCityRecord(String cityName, String countryName, double latitude, double longitude) {
}
