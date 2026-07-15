package com.guentours.provider.sabre;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Request body for Sabre's Get Hotel Details v5 endpoint ({@code POST /v5/get/hoteldetails}),
 * matching a verified real sample from Sabre's official 2025.09 Lodging Postman collection.
 * Used right before booking (or on-demand price re-check) to fetch the property's current
 * bookable rate plans for a specific {@code HotelCode}, one of which is then passed to
 * Hotel Price Check to obtain a fresh {@code BookingKey}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record SabreHotelDetailsRequest(GetHotelDetailsRQ GetHotelDetailsRQ) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GetHotelDetailsRQ(POS POS, SearchCriteria SearchCriteria) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record POS(Source Source) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Source(String PseudoCityCode) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record SearchCriteria(HotelRefs HotelRefs, RateInfoRef RateInfoRef) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record HotelRefs(HotelRef HotelRef) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record HotelRef(String HotelCode, String CodeContext) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record RateInfoRef(Rooms Rooms, StayDateTimeRange StayDateTimeRange, String CurrencyCode, String RateSource) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Rooms(List<Room> Room) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Room(int Index, int Adults) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record StayDateTimeRange(String StartDate, String EndDate) {
    }
}
