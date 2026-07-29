package com.guentours.provider.travelopro;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the shape of Travelopro's {@code aeroVE5/booking} request: auth fields at the root (matching
 * every other endpoint in this adapter, added after "Invalid JSON request" persisted without them),
 * a {@code flightBookingInfo}/{@code paxInfo} envelope, and passengers split by type ({@code adult}/
 * {@code child}/{@code infant}) into column-oriented arrays where values at the same index belong to
 * the same traveler.
 */
class TraveloproFlightBookRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializesAuthFieldsAtTheRootAndTheFlightBookingInfoEnvelope() throws Exception {
        var flightBookingInfo = new TraveloproBookRequest.FlightBookingInfo(
                "sess-123", "FARE-XYZ", "true", "010", "237", "Private", "");
        var paxInfo = new TraveloproBookRequest.PaxInfo("GT-abc", "0000", "jane@example.com",
                "+237600000000", "",
                List.of(new TraveloproBookRequest.PaxDetail(
                        new TraveloproBookRequest.PaxGroup(
                                List.of("Mr"), List.of("Jane"), List.of("Traveler"),
                                List.of("1990-01-01"), List.of("FR"), List.of("X123456"),
                                List.of("FR"), List.of("2030-01-01")),
                        null, null)));
        var request = new TraveloproBookRequest("uid", "pwd", "Test", "127.0.0.1", flightBookingInfo, paxInfo);

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"user_id\":\"uid\"");
        assertThat(json).contains("\"user_password\":\"pwd\"");
        assertThat(json).contains("\"access\":\"Test\"");
        assertThat(json).contains("\"ip_address\":\"127.0.0.1\"");

        assertThat(json).contains("\"flightBookingInfo\"");
        assertThat(json).contains("\"flight_session_id\":\"sess-123\"");
        assertThat(json).contains("\"fare_source_code\":\"FARE-XYZ\"");
        assertThat(json).contains("\"IsPassportMandatory\":\"true\"");
        assertThat(json).contains("\"fareType\":\"Private\"");

        assertThat(json).contains("\"paxInfo\"");
        assertThat(json).contains("\"customerEmail\":\"jane@example.com\"");
        assertThat(json).contains("\"customerPhone\":\"+237600000000\"");
        assertThat(json).contains("\"paxDetails\"");
        assertThat(json).contains("\"adult\"");
        assertThat(json).contains("\"firstName\":[\"Jane\"]");
        assertThat(json).contains("\"nationality\":[\"FR\"]");
        // A null PaxGroup (as opposed to an empty one - see TraveloproClient.buildFlightPaxGroup,
        // which always builds a real, if empty, group) is omitted rather than serialized as null.
        assertThat(json).doesNotContain("\"child\"").doesNotContain("\"infant\"");
    }

    @Test
    void groupsAdultAndChildPassengersIntoSeparateColumnOrientedArrays() throws Exception {
        var adult = new TraveloproBookRequest.PaxGroup(
                List.of("Mr", "Mrs"), List.of("Paul", "Anne"), List.of("Dupont", "Dupont"),
                List.of("1980-05-01", "1982-07-01"), List.of("CM", "CM"), List.of("P1", "P2"),
                List.of("CM", "CM"), List.of("2030-01-01", "2031-01-01"));
        var child = new TraveloproBookRequest.PaxGroup(
                List.of("Master"), List.of("Leo"), List.of("Dupont"),
                List.of("2015-03-01"), List.of("CM"), List.of("P3"),
                List.of("CM"), List.of("2032-01-01"));
        var paxDetail = new TraveloproBookRequest.PaxDetail(adult, child, null);
        var request = new TraveloproBookRequest(
                "uid", "pwd", "Test", "127.0.0.1",
                new TraveloproBookRequest.FlightBookingInfo("sess-123", "FARE-XYZ", "false", "010", "237", "Public", ""),
                new TraveloproBookRequest.PaxInfo(null, "0000", "a@b.com", "600000000", "", List.of(paxDetail)));

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"firstName\":[\"Paul\",\"Anne\"]");
        assertThat(json).contains("\"firstName\":[\"Leo\"]");
        assertThat(json).doesNotContain("\"infant\"");
    }

    @Test
    void deserializesTheVendorsSuccessBookingResponse() throws Exception {
        String json = """
                {
                    "BookFlightResponse": {
                        "BookFlightResult":  {
                            "Errors":  "",
                            "Status":  "CONFIRMED",
                            "Success":  true,
                            "Target":  "Test",
                            "TktTimeLimit":  "2022-11-13T12:20:07",
                            "UniqueID":  "TR31072022"
                        }
                    }
                }
                """;

        TraveloproBookResponse response = mapper.readValue(json, TraveloproBookResponse.class);
        var result = response.BookFlightResponse().BookFlightResult();

        assertThat(result.Status()).isEqualTo("CONFIRMED");
        assertThat(result.UniqueID()).isEqualTo("TR31072022");
        assertThat(result.TktTimeLimit()).isEqualTo("2022-11-13T12:20:07");
        assertThat(result.firstErrorMessage()).isNull();
    }

    @Test
    void deserializesTheVendorsFailureBookingResponseAndExposesTheRealError() throws Exception {
        String json = """
                {
                    "BookFlightResponse":
                    {
                        "BookFlightResult":
                        {
                            "Errors":  [
                                {
                                    "Errors":
                                    {
                                        "ErrorCode":  "ERBUK108",
                                        "ErrorMessage":  "PassengerNationality details is required for this airline."
                                    }
                                }
                            ],
                            "Status":  "",
                            "Success":  "false",
                            "Target":  "Test",
                            "TktTimeLimit":  "",
                            "UniqueID":  ""
                        }
                    }
                }
                """;

        TraveloproBookResponse response = mapper.readValue(json, TraveloproBookResponse.class);
        var result = response.BookFlightResponse().BookFlightResult();

        assertThat(result.Status()).isEmpty();
        assertThat(result.firstErrorMessage()).isEqualTo("ERBUK108: PassengerNationality details is required for this airline.");
    }

    @Test
    void treatsAPlainStringErrorsAsTheErrorMessageDirectly() throws Exception {
        // Some GDS reference implementations report Errors coming back as a plain non-empty string
        // for certain failure classes, rather than the structured {ErrorCode,ErrorMessage} wrapper.
        String json = """
                {
                    "BookFlightResponse": {
                        "BookFlightResult": {
                            "Errors": "Session expired",
                            "Status": "",
                            "UniqueID": ""
                        }
                    }
                }
                """;

        TraveloproBookResponse response = mapper.readValue(json, TraveloproBookResponse.class);
        var result = response.BookFlightResponse().BookFlightResult();

        assertThat(result.firstErrorMessage()).isEqualTo("Session expired");
    }

    @Test
    void deserializesATopLevelValidationErrorWithNoBookFlightResponseWrapper() throws Exception {
        // Real sandbox response for a request missing a required field - a completely different,
        // unwrapped shape from the BookFlightResponse.BookFlightResult envelope used above.
        String json = """
                {"Errors":{"ErrorCode":"FLTBOOKVAL","ErrorMessage":"countryCode required"}}
                """;

        TraveloproBookResponse response = mapper.readValue(json, TraveloproBookResponse.class);

        assertThat(response.BookFlightResponse()).isNull();
        assertThat(response.Errors().describe()).isEqualTo("FLTBOOKVAL: countryCode required");
    }
}
