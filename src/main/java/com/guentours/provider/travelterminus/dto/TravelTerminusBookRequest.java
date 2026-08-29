package com.guentours.provider.travelterminus.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/** Body for {@code POST /api/flights/book} - the final step. Since the 2026-08-13 breaking
 *  change this is immediate: it debits the Travel Terminus wallet and issues the e-ticket in one
 *  call, there is no more "hold" state (see the API changelog). {@code flightObject} must be the
 *  one returned by Revalidate, not the one from Search/Branded Fare. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TravelTerminusBookRequest(
        String searchReqId,
        String hashReqKey,
        JsonNode flightObject,
        String endUserIP,
        String endUserBrowserAgent,
        List<Passenger> passengers,
        Contact contact
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Passenger(
            String passengerType,
            String gender,
            String title,
            String firstName,
            String lastName,
            String dateOfBirth,
            String nationalityCode,
            String countryCode,
            String city,
            String mobile,
            String panCardNumber,
            Document document,
            /** From Pre Ancillary's baggages[].bagsData[].passengers[].flightObject, passed through
             *  unmodified - see the "Connecting to the Book API" doc section. Omitted (not an empty
             *  array) when no baggage was picked - the docs' own example always includes the key,
             *  but NON_NULL keeps this consistent with every other optional field on this record. */
            List<AncillaryRef> baggages,
            List<AncillaryRef> meals,
            List<AncillaryRef> seats
    ) {
    }

    /** One selected ancillary, echoed back exactly as received in a Pre Ancillary passenger
     *  entry's {@code flightObject} ({@code offerId}+{@code paxRef}) - see
     *  TravelTerminusPreAncillaryResponse.PaxPrice. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AncillaryRef(String offerId, String paxRef) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Document(String documentType, String documentNumber, String issueDate, String expiryDate, String country) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Contact(String email, String mobile, String mobileCountryCode, Gst gst) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Gst(String gstNumber, String gstCompanyName, String gstCompanyEmail, String gstCompanyAddress, String gstCompanyContactNumber) {
    }
}
