package com.guentours.provider.sabre;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request body for Sabre's Hotel Price Check v5 endpoint ({@code POST /v5/hotel/pricecheck}) -
 * endpoint confirmed against Sabre's official 2025.09 Lodging Postman collection ("Verify
 * selected rate and return up-to-date pricing and the BookingKey for booking"). Re-prices the
 * rate plan picked from Get Hotel Details right before booking; the response's
 * {@code BookingKey} is what {@code createBooking} then consumes.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record SabreHotelPriceCheckRequest(HotelPriceCheckRQ HotelPriceCheckRQ) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record HotelPriceCheckRQ(POS POS, HotelRateSelect HotelRateSelect) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record POS(Source Source) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Source(String PseudoCityCode) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record HotelRateSelect(String HotelCode, String RatePlanCode, StayDateTimeRange StayDateTimeRange) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record StayDateTimeRange(String StartDate, String EndDate) {
    }
}
