package com.guentours.provider.sabre;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Request body for Sabre's Get Hotel Avail v5 endpoint ({@code POST /v5/get/hotelavail}),
 * matching a verified real sample from Sabre's official 2025.09 Lodging (Content Services
 * for Lodging) Postman collection: {@code POS.Source} carries the PCC, {@code GeoSearch}
 * resolves the city by IATA code ({@code RefPointType "6"}), and {@code RateInfoRef} carries
 * the stay dates, currency and room occupancy.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record SabreHotelAvailRequest(GetHotelAvailRQ GetHotelAvailRQ) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GetHotelAvailRQ(POS POS, SearchCriteria SearchCriteria) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record POS(Source Source) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Source(String PseudoCityCode) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record SearchCriteria(
            int PageSize,
            String SortBy,
            String SortOrder,
            boolean RateDetailsInd,
            GeoSearch GeoSearch,
            RateInfoRef RateInfoRef
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GeoSearch(GeoRef GeoRef) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GeoRef(int Radius, String UOM, RefPoint RefPoint) {
    }

    /** {@code RefPointType "6"} resolves the reference point by IATA city/airport code. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record RefPoint(String Value, String ValueContext, String RefPointType) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record RateInfoRef(String CurrencyCode, String BestOnly, StayDateTimeRange StayDateTimeRange, Rooms Rooms) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record StayDateTimeRange(String StartDate, String EndDate) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Rooms(List<Room> Room) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Room(int Index, int Adults) {
    }
}
