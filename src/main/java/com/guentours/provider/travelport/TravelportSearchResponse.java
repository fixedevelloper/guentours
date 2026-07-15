package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response body of Travelport's JSON air-shopping endpoint
 * ({@code POST /air/catalog/search/catalogproductofferings}), matching a verified real response.
 * Reference-heavy: each {@code CatalogProductOffering} exposes {@code ProductBrandOptions} whose
 * {@code flightRefs} point at flight entries kept in a separate {@code ReferenceList} of type
 * {@code ReferenceListFlight} - the two must be cross-referenced by flight id to resolve an
 * offering's real flights. {@code Result.Error} carries per-request errors ("No flights found").
 *
 * <p>The verified sample was a mock with placeholder values, so the internals of the priced
 * {@code ProductBrandOffering.Price} and of each {@code ReferenceListFlight.Flight} entry were
 * not fully populated; those field names still need confirming against a real priced response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TravelportSearchResponse(CatalogProductOfferingsResponse CatalogProductOfferingsResponse) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CatalogProductOfferingsResponse(
            String transactionId,
            CatalogProductOfferings CatalogProductOfferings,
            List<ReferenceList> ReferenceList,
            Result Result
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CatalogProductOfferings(
            String id,
            Identifier Identifier,
            List<CatalogProductOffering> CatalogProductOffering
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CatalogProductOffering(
            String id,
            Identifier Identifier,
            Integer sequence,
            String Departure,
            String Arrival,
            List<ProductBrandOptions> ProductBrandOptions
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Identifier(String value, String authority) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ProductBrandOptions(
            List<String> flightRefs,
            List<ProductBrandOffering> ProductBrandOffering
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ProductBrandOffering(Price Price) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Price(Double TotalPrice, String CurrencyCode) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ReferenceList(
            @JsonProperty("@type") String type,
            List<Flight> Flight
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Flight(
            String id,
            String carrier,
            String number,
            String classOfService,
            Endpoint Departure,
            Endpoint Arrival
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Endpoint(String location, String date, String time) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Result(String status, List<ErrorDetail> Error) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ErrorDetail(Integer StatusCode, String Message) {
    }
}
