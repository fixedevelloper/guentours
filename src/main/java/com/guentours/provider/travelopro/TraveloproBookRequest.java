package com.guentours.provider.travelopro;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Request body for Travelopro's {@code /api/aeroVE5/booking} endpoint - creates the reservation
 * (PNR) against the fare quoted at search time and revalidated just before this call. Auth fields
 * (user_id/user_password/access/ip_address) are sent at the root, matching every other Travelopro
 * endpoint in this adapter (hotel_search, hotel_book, availability, revalidate, ticket) - the
 * Booking API docs' parameter table doesn't list them, but a reference implementation confirmed to
 * work against this vendor includes them, and their complete absence is a plausible cause of the
 * generic "Invalid JSON request" rejection seen without them. flightBookingInfo/paxInfo carry the
 * flight-specific fields, with passengers split by type ({@code adult}/{@code child}/{@code infant})
 * into column-oriented arrays (one array per field, values aligned by index) - the same convention
 * {@code hotel_book}'s {@code PaxNames} already uses.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TraveloproBookRequest(
        String user_id,
        String user_password,
        String access,
        String ip_address,
        FlightBookingInfo flightBookingInfo,
        PaxInfo paxInfo
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record FlightBookingInfo(
            String flight_session_id,
            String fare_source_code,
            /** Sent as the string "true"/"false" (not a JSON boolean) - matches the vendor's own sample request. */
            String IsPassportMandatory,
            String areaCode,
            String countryCode,
            String fareType,
            /** Return-leg fare source code for a round trip; empty string (not omitted) for one-way. */
            String fare_source_code_inbound
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PaxInfo(
            String clientRef,
            String postCode,
            String customerEmail,
            String customerPhone,
            String bookingNote,
            List<PaxDetail> paxDetails
    ) {
    }

    /** First (and only) index of paxDetails: one group per passenger type present in the booking. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PaxDetail(PaxGroup adult, PaxGroup child, PaxGroup infant) {
    }

    /** Column-oriented per-type passenger arrays: values at the same index belong to the same traveler. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PaxGroup(
            List<String> title,
            List<String> firstName,
            List<String> lastName,
            List<String> dob,
            List<String> nationality,
            List<String> passportNo,
            List<String> passportIssueCountry,
            List<String> passportExpiryDate
    ) {
    }
}
