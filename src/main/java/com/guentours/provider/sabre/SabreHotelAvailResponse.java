package com.guentours.provider.sabre;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response body of Sabre's Get Hotel Avail v5 endpoint ({@code POST /v5/get/hotelavail}).
 * The endpoint itself and the request shape are confirmed against Sabre's official 2025.09
 * Lodging Postman collection; this response follows Sabre's long-standing OTA-derived hotel
 * availability naming ({@code HotelInfo}/{@code RateInfo}/{@code ConvertedRateInfo}) used
 * consistently across their hotel APIs. Every field is defensively nullable - verify against
 * a live CERT response before disabling mock mode.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record SabreHotelAvailResponse(GetHotelAvailRS GetHotelAvailRS) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GetHotelAvailRS(HotelAvailInfos HotelAvailInfos) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record HotelAvailInfos(List<HotelAvailInfo> HotelAvailInfo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record HotelAvailInfo(HotelInfo HotelInfo, RateInfos RateInfos) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record HotelInfo(String HotelCode, String HotelName) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RateInfos(List<RateInfo> RateInfo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RateInfo(String RatePlanCode, String RateSource, ConvertedRateInfo ConvertedRateInfo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ConvertedRateInfo(Double AmountAfterTax, String CurrencyCode) {
    }
}
