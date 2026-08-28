package com.guentours.provider.travelterminus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * {@code data} of the Revalidate response: the refreshed {@code route} (final price/availability,
 * and - crucially - the {@code flightObject} that must be saved and sent unmodified to Book) plus
 * {@code bookingRequiredValidation}, which drives which passenger fields are mandatory for Book.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TravelTerminusRevalidateResponse(
        String status,
        String searchReqId,
        String hashReqKey,
        BookingRequiredValidation bookingRequiredValidation,
        TravelTerminusRoute route
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PerPaxTypeFlag(Boolean adult, Boolean child, Boolean infant) {
    }

    /** See the "Important Rules for Foreign Nationals & Compliance" note in the Revalidate docs:
     *  {@code isPanRequired}+{@code allowPassportInsteadOfPan}+{@code isPassportImageRequired}
     *  together decide whether a passenger needs a PAN, a passport, or both are refused. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BookingRequiredValidation(
            Boolean passportRequired,
            Boolean isGSTRequired,
            Boolean gstAllowed,
            PerPaxTypeFlag isPanRequired,
            Boolean isPassportImageRequired,
            Boolean allowPassportInsteadOfPan,
            Boolean seatAncillaryRequiresPassengers
    ) {
    }
}
