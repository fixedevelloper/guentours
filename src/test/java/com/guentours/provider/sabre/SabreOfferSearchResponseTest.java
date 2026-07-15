package com.guentours.provider.sabre;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies our DTOs match the real Sabre Bargain Finder Max v5 ({@code POST /v5/offers/shop})
 * wire format - trimmed sample of an actual response (WAW-SPU round trip on LO). A mismatch
 * here would silently drop fields as null rather than fail loudly, so this pins the exact
 * JSON shape down: numeric ids/refs, schedule times as time-of-day with UTC offset, dates in
 * the group's legDescriptions, and seats/cabin inside the pricing fare components.
 */
class SabreOfferSearchResponseTest {

    private static final String SAMPLE_RESPONSE = """
            {
                "groupedItineraryResponse": {
                    "version": "5",
                    "statistics": { "itineraryCount": 1 },
                    "scheduleDescs": [
                        {
                            "id": 1,
                            "frequency": "SM*W***",
                            "stopCount": 0,
                            "eTicketable": true,
                            "totalMilesFlown": 635,
                            "elapsedTime": 115,
                            "departure": { "airport": "SPU", "city": "SPU", "country": "HR", "time": "17:10:00+02:00" },
                            "arrival": { "airport": "WAW", "city": "WAW", "country": "PL", "time": "19:05:00+02:00" },
                            "carrier": {
                                "marketing": "LO",
                                "marketingFlightNumber": 576,
                                "operating": "LO",
                                "operatingFlightNumber": 576,
                                "equipment": { "code": "E75", "typeForFirstLeg": "N", "typeForLastLeg": "N" }
                            }
                        },
                        {
                            "id": 2,
                            "frequency": "SM*W***",
                            "stopCount": 0,
                            "eTicketable": true,
                            "totalMilesFlown": 635,
                            "elapsedTime": 120,
                            "departure": { "airport": "WAW", "city": "WAW", "country": "PL", "time": "14:20:00+02:00" },
                            "arrival": { "airport": "SPU", "city": "SPU", "country": "HR", "time": "16:20:00+02:00" },
                            "carrier": {
                                "marketing": "LO",
                                "marketingFlightNumber": 575,
                                "operating": "LO",
                                "operatingFlightNumber": 575,
                                "equipment": { "code": "E75", "typeForFirstLeg": "N", "typeForLastLeg": "N" }
                            }
                        }
                    ],
                    "legDescs": [
                        { "id": 1, "elapsedTime": 115, "schedules": [ { "ref": 1 } ] },
                        { "id": 2, "elapsedTime": 120, "schedules": [ { "ref": 2 } ] }
                    ],
                    "itineraryGroups": [
                        {
                            "groupDescription": {
                                "legDescriptions": [
                                    { "departureDate": "2026-09-11", "departureLocation": "WAW", "arrivalLocation": "SPU" },
                                    { "departureDate": "2026-09-18", "departureLocation": "SPU", "arrivalLocation": "WAW" }
                                ]
                            },
                            "itineraries": [
                                {
                                    "id": 1,
                                    "pricingSource": "ADVJR1",
                                    "legs": [ { "ref": 2 }, { "ref": 1 } ],
                                    "pricingInformation": [
                                        {
                                            "pricingSubsource": "HPIS",
                                            "fare": {
                                                "mandatoryInd": true,
                                                "validatingCarrierCode": "LO",
                                                "vita": true,
                                                "eTicketable": true,
                                                "lastTicketDate": "2026-11-21",
                                                "lastTicketTime": "12:29",
                                                "governingCarriers": "LO LO",
                                                "passengerInfoList": [
                                                    {
                                                        "passengerInfo": {
                                                            "passengerType": "ADT",
                                                            "passengerNumber": 1,
                                                            "nonRefundable": true,
                                                            "fareComponents": [
                                                                {
                                                                    "ref": 1,
                                                                    "beginAirport": "WAW",
                                                                    "endAirport": "SPU",
                                                                    "segments": [
                                                                        {
                                                                            "segment": {
                                                                                "bookingCode": "O",
                                                                                "cabinCode": "Y",
                                                                                "mealCode": "RF",
                                                                                "seatsAvailable": 9,
                                                                                "availabilityBreak": true
                                                                            }
                                                                        }
                                                                    ]
                                                                },
                                                                {
                                                                    "ref": 2,
                                                                    "beginAirport": "SPU",
                                                                    "endAirport": "WAW",
                                                                    "segments": [
                                                                        {
                                                                            "segment": {
                                                                                "bookingCode": "O",
                                                                                "cabinCode": "Y",
                                                                                "mealCode": "RF",
                                                                                "seatsAvailable": 4,
                                                                                "availabilityBreak": true
                                                                            }
                                                                        }
                                                                    ]
                                                                }
                                                            ],
                                                            "passengerTotalFare": {
                                                                "totalFare": 131.8,
                                                                "totalTaxAmount": 73.8,
                                                                "currency": "USD"
                                                            }
                                                        }
                                                    }
                                                ],
                                                "totalFare": {
                                                    "totalPrice": 131.8,
                                                    "totalTaxAmount": 73.8,
                                                    "currency": "USD",
                                                    "baseFareAmount": 235,
                                                    "baseFareCurrency": "PLN",
                                                    "equivalentAmount": 58,
                                                    "equivalentCurrency": "USD"
                                                }
                                            }
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            }
            """;

    @Test
    void deserializesTheRealWireShape() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SabreOfferSearchResponse response = mapper.readValue(SAMPLE_RESPONSE, SabreOfferSearchResponse.class);

        var body = response.groupedItineraryResponse();
        assertThat(body).isNotNull();

        assertThat(body.scheduleDescs()).hasSize(2);
        var outboundSchedule = body.scheduleDescs().get(1);
        assertThat(outboundSchedule.id()).isEqualTo(2);
        assertThat(outboundSchedule.departure().airport()).isEqualTo("WAW");
        assertThat(outboundSchedule.departure().time()).isEqualTo("14:20:00+02:00");
        assertThat(outboundSchedule.arrival().airport()).isEqualTo("SPU");
        assertThat(outboundSchedule.arrival().time()).isEqualTo("16:20:00+02:00");
        assertThat(outboundSchedule.carrier().marketing()).isEqualTo("LO");
        assertThat(outboundSchedule.carrier().marketingFlightNumber()).isEqualTo(575);
        assertThat(outboundSchedule.eTicketable()).isTrue();

        assertThat(body.legDescs()).hasSize(2);
        assertThat(body.legDescs().get(1).id()).isEqualTo(2);
        assertThat(body.legDescs().get(1).schedules()).extracting(SabreOfferSearchResponse.Ref::ref)
                .containsExactly(2);

        var group = body.itineraryGroups().get(0);
        assertThat(group.groupDescription().legDescriptions()).hasSize(2);
        assertThat(group.groupDescription().legDescriptions().get(0).departureDate()).isEqualTo("2026-09-11");
        assertThat(group.groupDescription().legDescriptions().get(0).departureLocation()).isEqualTo("WAW");

        var itinerary = group.itineraries().get(0);
        assertThat(itinerary.id()).isEqualTo(1);
        // The first legs entry is the OUTBOUND leg (WAW->SPU = leg 2 here), matching legDescriptions order.
        assertThat(itinerary.legs()).extracting(SabreOfferSearchResponse.Ref::ref).containsExactly(2, 1);

        var fare = itinerary.pricingInformation().get(0).fare();
        assertThat(fare.validatingCarrierCode()).isEqualTo("LO");
        assertThat(fare.lastTicketDate()).isEqualTo("2026-11-21");
        assertThat(fare.totalFare().totalPrice()).isEqualTo(131.8);
        assertThat(fare.totalFare().currency()).isEqualTo("USD");

        var passengerInfo = fare.passengerInfoList().get(0).passengerInfo();
        assertThat(passengerInfo.passengerType()).isEqualTo("ADT");
        var firstSegment = passengerInfo.fareComponents().get(0).segments().get(0).segment();
        assertThat(firstSegment.cabinCode()).isEqualTo("Y");
        assertThat(firstSegment.seatsAvailable()).isEqualTo(9);
        assertThat(passengerInfo.fareComponents().get(1).segments().get(0).segment().seatsAvailable()).isEqualTo(4);
    }

    @Test
    void serializesARoundTripSearchRequestMatchingTheVerifiedSample() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var source = new SabreOfferSearchRequest.Source(
                "XXXX",
                new SabreOfferSearchRequest.RequestorID("1", "1", new SabreOfferSearchRequest.CompanyName("TN")));
        var rq = new SabreOfferSearchRequest.OTA_AirLowFareSearchRQ(
                "5",
                new SabreOfferSearchRequest.POS(java.util.List.of(source)),
                java.util.List.of(
                        new SabreOfferSearchRequest.OriginDestinationInformation(
                                "2026-09-11T00:00:00",
                                new SabreOfferSearchRequest.Location("WAW"),
                                new SabreOfferSearchRequest.Location("SPU"),
                                null),
                        new SabreOfferSearchRequest.OriginDestinationInformation(
                                "2026-09-18T00:00:00",
                                new SabreOfferSearchRequest.Location("SPU"),
                                new SabreOfferSearchRequest.Location("WAW"),
                                null)),
                new SabreOfferSearchRequest.TravelPreferences(java.util.List.of(
                        new SabreOfferSearchRequest.CabinPref("Y", "Preferred"))),
                new SabreOfferSearchRequest.TravelerInfoSummary(
                        java.util.List.of(new SabreOfferSearchRequest.AirTravelerAvail(java.util.List.of(
                                new SabreOfferSearchRequest.PassengerTypeQuantity("ADT", 1)))),
                        new SabreOfferSearchRequest.PriceRequestInformation("USD")),
                new SabreOfferSearchRequest.TPA_Extensions(new SabreOfferSearchRequest.IntelliSellTransaction(
                        new SabreOfferSearchRequest.RequestType("50ITINS"))));

        String json = mapper.writeValueAsString(new SabreOfferSearchRequest(rq));

        assertThat(json).contains("\"Version\":\"5\"");
        assertThat(json).contains("\"PseudoCityCode\":\"XXXX\"");
        assertThat(json).contains("\"RequestorID\":{\"Type\":\"1\",\"ID\":\"1\",\"CompanyName\":{\"Code\":\"TN\"}}");
        assertThat(json).contains("\"DepartureDateTime\":\"2026-09-11T00:00:00\"");
        assertThat(json).contains("\"LocationCode\":\"WAW\"");
        assertThat(json).contains("\"Code\":\"ADT\"");
        assertThat(json).contains("\"Quantity\":1");
        assertThat(json).contains("\"IntelliSellTransaction\":{\"RequestType\":{\"Name\":\"50ITINS\"}}");
    }
}
