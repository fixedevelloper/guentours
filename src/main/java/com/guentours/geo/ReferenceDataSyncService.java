package com.guentours.geo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reloads the airports/hotel_cities reference tables from their configured
 * {@link AirportDataSource}/{@link HotelCityDataSource}. Airports upsert cleanly via their
 * natural key (the IATA code is the primary key, so {@code save()} updates in place);
 * hotel cities have a generated id, so existing rows are looked up by (city, country) and
 * refreshed in place instead of being duplicated on every sync.
 */
@Service
public class ReferenceDataSyncService {

    private static final Logger log = LoggerFactory.getLogger(ReferenceDataSyncService.class);

    private final AirportDataSource airportDataSource;
    private final HotelCityDataSource hotelCityDataSource;
    private final AirportRepository airportRepository;
    private final HotelCityRepository hotelCityRepository;

    public ReferenceDataSyncService(AirportDataSource airportDataSource, HotelCityDataSource hotelCityDataSource,
                                     AirportRepository airportRepository, HotelCityRepository hotelCityRepository) {
        this.airportDataSource = airportDataSource;
        this.hotelCityDataSource = hotelCityDataSource;
        this.airportRepository = airportRepository;
        this.hotelCityRepository = hotelCityRepository;
    }

    @Transactional
    public int syncAirports() {
        List<AirportRecord> records = airportDataSource.fetchAll();
        List<Airport> airports = records.stream()
                .map(r -> new Airport(r.airportCode(), r.airportName(), r.city(), r.country()))
                .toList();
        airportRepository.saveAll(airports);
        log.info("Synced {} airports", airports.size());
        return airports.size();
    }

    @Transactional
    public int syncHotelCities() {
        List<HotelCityRecord> records = hotelCityDataSource.fetchAll();
        for (HotelCityRecord record : records) {
            HotelCity city = hotelCityRepository
                    .findByCityNameIgnoreCaseAndCountryNameIgnoreCase(record.cityName(), record.countryName())
                    .orElseGet(() -> new HotelCity(record.cityName(), record.countryName(), record.latitude(), record.longitude()));
            city.refresh(record.cityName(), record.countryName(), record.latitude(), record.longitude());
            hotelCityRepository.save(city);
        }
        log.info("Synced {} hotel cities", records.size());
        return records.size();
    }
}
