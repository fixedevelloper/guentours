package com.guentours.provider.sabre;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response body of Sabre's Hotel Price Check v5 endpoint ({@code POST /v5/hotel/pricecheck}).
 * {@code BookingKey} is a directly confirmed field name (per Sabre's own product
 * description: "...return up-to-date pricing and the BookingKey for booking"); the rest of
 * the price nesting follows Sabre's OTA-derived rate naming and should be verified against a
 * live CERT response before disabling mock mode.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record SabreHotelPriceCheckResponse(HotelPriceCheckRS HotelPriceCheckRS) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record HotelPriceCheckRS(String BookingKey, boolean PriceMatch, ConvertedRateInfo ConvertedRateInfo,
                              CancelPolicy CancelPolicy) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ConvertedRateInfo(Double AmountAfterTax, String CurrencyCode) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CancelPolicy(String Description) {
    }
}
