package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request body for Travelport's reservation-creation endpoint
 * ({@code POST /book/reservation/reservations}), which books the priced offer and returns a
 * PNR/locator. References the priced {@code Offer} id(s) from the preceding pricing step and
 * carries the travelers and contact details. Field names follow Travelport's documented JSON
 * reservation shape; identity documents are not sent yet pending the exact spec.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TravelportReservationRequest(ReservationDetail ReservationDetail) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ReservationDetail(
            @JsonProperty("@type") String type,
            List<OfferRef> Offer,
            List<Traveler> Traveler,
            ContactInfo ContactInfo
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record OfferRef(String id) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Traveler(
            @JsonProperty("@type") String type,
            String givenName,
            String surname,
            String passengerTypeCode,
            String birthDate
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ContactInfo(List<Email> Email) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Email(String value) {
    }
}
