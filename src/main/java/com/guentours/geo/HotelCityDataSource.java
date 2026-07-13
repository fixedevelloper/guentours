package com.guentours.geo;

import java.util.List;

/**
 * Single-Provider-Interface for wherever hotel-city reference data actually comes from.
 * {@link SeedHotelCityDataSource} is the only implementation today (a curated static list),
 * kept deliberately swappable the same way {@link AirportDataSource} is.
 */
public interface HotelCityDataSource {

    List<HotelCityRecord> fetchAll();
}
