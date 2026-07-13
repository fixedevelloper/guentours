package com.guentours.geo;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/** Backs the frontend's origin/destination/city autocomplete: requires at least 2 characters. */
@Service
public class GeoSearchService {

    private static final int MIN_QUERY_LENGTH = 2;
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final AirportRepository airportRepository;
    private final HotelCityRepository hotelCityRepository;

    public GeoSearchService(AirportRepository airportRepository, HotelCityRepository hotelCityRepository) {
        this.airportRepository = airportRepository;
        this.hotelCityRepository = hotelCityRepository;
    }

    public List<AirportResponse> searchAirports(String query, Integer limit) {
        String q = validate(query);
        return airportRepository.search(q, PageRequest.of(0, resolveLimit(limit))).stream()
                .map(AirportResponse::from)
                .toList();
    }

    public List<HotelCityResponse> searchCities(String query, Integer limit) {
        String q = validate(query);
        return hotelCityRepository.search(q, PageRequest.of(0, resolveLimit(limit))).stream()
                .map(HotelCityResponse::from)
                .toList();
    }

    private String validate(String query) {
        if (query == null || query.trim().length() < MIN_QUERY_LENGTH) {
            throw new IllegalArgumentException("query must be at least " + MIN_QUERY_LENGTH + " characters");
        }
        return query.trim();
    }

    private int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
