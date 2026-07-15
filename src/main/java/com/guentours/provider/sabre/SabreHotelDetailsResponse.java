package com.guentours.provider.sabre;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response body of Sabre's Get Hotel Details v5 endpoint ({@code POST /v5/get/hoteldetails}).
 * The endpoint and request shape are confirmed against Sabre's official 2025.09 Lodging
 * Postman collection; this response's nesting follows Sabre's OTA-derived hotel rate-plan
 * naming. {@code RatePlanCode} is the identifier Hotel Price Check re-prices against - every
 * field is defensively nullable and should be verified against a live CERT response before
 * disabling mock mode.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record SabreHotelDetailsResponse(GetHotelDetailsRS GetHotelDetailsRS) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GetHotelDetailsRS(HotelDetailsInfo HotelDetailsInfo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record HotelDetailsInfo(HotelRateInfo HotelRateInfo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record HotelRateInfo(RatePlans RatePlans) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RatePlans(List<RatePlan> RatePlan) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RatePlan(String RatePlanCode, ConvertedRateInfo ConvertedRateInfo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ConvertedRateInfo(Double AmountAfterTax, String CurrencyCode) {
    }
}
