package com.guentours.provider;

import java.time.LocalDate;

/**
 * {@code latitude}/{@code longitude} are the searched city's coordinates, resolved upstream
 * (see {@code HotelSearchService}) from our own hotel-city reference data - the city
 * autocomplete a traveler picks from carries no IATA-style code, only a name, so a provider
 * that needs geo-based search (e.g. Travelport) cannot rely on {@code cityCode} alone. Null
 * when the city has no matching reference row; providers should fall back to a
 * name/code-based search in that case.
 */
public record HotelSearchCriteria(
        String cityCode,
        LocalDate checkIn,
        LocalDate checkOut,
        int adults,
        int rooms,
        String currency,
        Double latitude,
        Double longitude
) {
}
