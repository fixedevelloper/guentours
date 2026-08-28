package com.guentours.provider.travelterminus.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Fare tier shape shared by the Search and Revalidate {@code route.fare[]} arrays. A live capture
 * of the Search response uses the abbreviated field names ({@code perAdtBaseFare}, ...) while the
 * hand-written docs example uses the spelled-out names ({@code perAdultBaseFare}, ...) - the
 * {@code @JsonAlias} on each accepts either, since the two Travel Terminus sources disagree.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TravelTerminusFare(
        String fareQuote,
        String fareType,
        @JsonAlias("perAdultBaseFare") Double perAdtBaseFare,
        @JsonAlias("perAdultTax") Double perAdtTax,
        @JsonAlias("perChildBaseFare") Double perChdBaseFare,
        @JsonAlias("perChildTax") Double perChdTax,
        @JsonAlias("perInfantBaseFare") Double perInfBaseFare,
        @JsonAlias("perInfantTax") Double perInfTax,
        Double totalBaseFare,
        Double totalTax,
        Double totalFare,
        String branchCurrency,
        Double walletPoints
) {
}
