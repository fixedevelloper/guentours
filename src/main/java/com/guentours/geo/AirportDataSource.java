package com.guentours.geo;

import java.util.List;

/**
 * Single-Provider-Interface for wherever airport reference data actually comes from.
 * {@link SeedAirportDataSource} is the only implementation today (a curated static list),
 * kept deliberately swappable: once a real feed (OurAirports, an internal GDS reference
 * endpoint, etc.) is confirmed, add a new {@code @Component} implementing this interface
 * and mark it {@code @Primary} - {@link ReferenceDataSyncService} and the 30-day schedule
 * don't need to change at all.
 */
public interface AirportDataSource {

    List<AirportRecord> fetchAll();
}
