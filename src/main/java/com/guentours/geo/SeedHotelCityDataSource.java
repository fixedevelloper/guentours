package com.guentours.geo;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default {@link HotelCityDataSource}: a curated list of ~50 major world cities with real
 * coordinates. Exists so city search works out of the box with no external network
 * dependency; swap in a real feed (GeoNames, etc.) the same way described on
 * {@link AirportDataSource}.
 */
@Component
class SeedHotelCityDataSource implements HotelCityDataSource {

    private static final List<HotelCityRecord> CITIES = List.of(
            new HotelCityRecord("Paris", "France", 48.8566, 2.3522),
            new HotelCityRecord("New York", "United States", 40.7128, -74.0060),
            new HotelCityRecord("London", "United Kingdom", 51.5074, -0.1278),
            new HotelCityRecord("Tokyo", "Japan", 35.6762, 139.6503),
            new HotelCityRecord("Dubai", "United Arab Emirates", 25.2048, 55.2708),
            new HotelCityRecord("Singapore", "Singapore", 1.3521, 103.8198),
            new HotelCityRecord("Barcelona", "Spain", 41.3851, 2.1734),
            new HotelCityRecord("Rome", "Italy", 41.9028, 12.4964),
            new HotelCityRecord("Amsterdam", "Netherlands", 52.3676, 4.9041),
            new HotelCityRecord("Berlin", "Germany", 52.5200, 13.4050),
            new HotelCityRecord("Madrid", "Spain", 40.4168, -3.7038),
            new HotelCityRecord("Istanbul", "Turkey", 41.0082, 28.9784),
            new HotelCityRecord("Bangkok", "Thailand", 13.7563, 100.5018),
            new HotelCityRecord("Hong Kong", "Hong Kong", 22.3193, 114.1694),
            new HotelCityRecord("Sydney", "Australia", -33.8688, 151.2093),
            new HotelCityRecord("Los Angeles", "United States", 34.0522, -118.2437),
            new HotelCityRecord("Miami", "United States", 25.7617, -80.1918),
            new HotelCityRecord("Chicago", "United States", 41.8781, -87.6298),
            new HotelCityRecord("San Francisco", "United States", 37.7749, -122.4194),
            new HotelCityRecord("Las Vegas", "United States", 36.1699, -115.1398),
            new HotelCityRecord("Toronto", "Canada", 43.6532, -79.3832),
            new HotelCityRecord("Mexico City", "Mexico", 19.4326, -99.1332),
            new HotelCityRecord("Sao Paulo", "Brazil", -23.5505, -46.6333),
            new HotelCityRecord("Rio de Janeiro", "Brazil", -22.9068, -43.1729),
            new HotelCityRecord("Cairo", "Egypt", 30.0444, 31.2357),
            new HotelCityRecord("Cape Town", "South Africa", -33.9249, 18.4241),
            new HotelCityRecord("Nairobi", "Kenya", -1.2921, 36.8219),
            new HotelCityRecord("Mumbai", "India", 19.0760, 72.8777),
            new HotelCityRecord("Delhi", "India", 28.7041, 77.1025),
            new HotelCityRecord("Seoul", "South Korea", 37.5665, 126.9780),
            new HotelCityRecord("Beijing", "China", 39.9042, 116.4074),
            new HotelCityRecord("Shanghai", "China", 31.2304, 121.4737),
            new HotelCityRecord("Vienna", "Austria", 48.2082, 16.3738),
            new HotelCityRecord("Prague", "Czech Republic", 50.0755, 14.4378),
            new HotelCityRecord("Budapest", "Hungary", 47.4979, 19.0402),
            new HotelCityRecord("Lisbon", "Portugal", 38.7223, -9.1393),
            new HotelCityRecord("Dublin", "Ireland", 53.3498, -6.2603),
            new HotelCityRecord("Brussels", "Belgium", 50.8503, 4.3517),
            new HotelCityRecord("Copenhagen", "Denmark", 55.6761, 12.5683),
            new HotelCityRecord("Stockholm", "Sweden", 59.3293, 18.0686),
            new HotelCityRecord("Oslo", "Norway", 59.9139, 10.7522),
            new HotelCityRecord("Helsinki", "Finland", 60.1699, 24.9384),
            new HotelCityRecord("Athens", "Greece", 37.9838, 23.7275),
            new HotelCityRecord("Zurich", "Switzerland", 47.3769, 8.5417),
            new HotelCityRecord("Munich", "Germany", 48.1351, 11.5820),
            new HotelCityRecord("Milan", "Italy", 45.4642, 9.1900),
            new HotelCityRecord("Venice", "Italy", 45.4408, 12.3155),
            new HotelCityRecord("Marrakech", "Morocco", 31.6295, -7.9811),
            new HotelCityRecord("Kuala Lumpur", "Malaysia", 3.1390, 101.6869),
            new HotelCityRecord("Manila", "Philippines", 14.5995, 120.9842),
            new HotelCityRecord("Auckland", "New Zealand", -36.8485, 174.7633),
            new HotelCityRecord("Melbourne", "Australia", -37.8136, 144.9631)
    );

    @Override
    public List<HotelCityRecord> fetchAll() {
        return CITIES;
    }
}
