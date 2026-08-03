package com.guentours.provider.travelport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the shape of Travelport's Stays Hotel Availability request/response against a verified real
 * sample: each {@code CatalogOffering} is a bookable rate whose {@code Price.TotalPrice} +
 * {@code CurrencyCode.value} give the fresh room rate.
 */
class TravelportHotelAvailabilityResponseTest {

    private static final String SAMPLE_RESPONSE = """
            {
                "CatalogOfferingsHospitalityResponse": {
                    "@type": "response",
                    "reservationStatus": "Success",
                    "CatalogOfferings": {
                        "@type": "CatalogOfferings",
                        "totalCatalogOffering": 1,
                        "catalogOfferingPerPage": 1,
                        "numberOfPages": 1,
                        "CatalogOffering": [
                            {
                                "@type": "CatalogOfferingHospitality",
                                "id": "108c5875",
                                "Identifier": { "value": "108c5875", "authority": "TVPT" },
                                "Price": {
                                    "@type": "PriceDetail",
                                    "CurrencyCode": { "value": "USD" },
                                    "Base": 20.2,
                                    "TotalTaxes": 34.4,
                                    "TotalFees": 201,
                                    "TotalPrice": 255.6
                                }
                            }
                        ]
                    },
                    "Result": { "@type": "Result", "status": "Complete" }
                }
            }
            """;

    @Test
    void deserializesTheAvailabilityWireShape() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        TravelportHotelAvailabilityResponse response =
                mapper.readValue(SAMPLE_RESPONSE, TravelportHotelAvailabilityResponse.class);

        var body = response.CatalogOfferingsHospitalityResponse();
        assertThat(body.reservationStatus()).isEqualTo("Success");
        assertThat(body.CatalogOfferings().totalCatalogOffering()).isEqualTo(1);
        var offering = body.CatalogOfferings().CatalogOffering().get(0);
        assertThat(offering.id()).isEqualTo("108c5875");
        assertThat(offering.Identifier().authority()).isEqualTo("TVPT");
        assertThat(offering.Price().TotalPrice()).isEqualTo(255.6);
        assertThat(offering.Price().CurrencyCode().value()).isEqualTo("USD");
    }

    private static final String SAMPLE_RESPONSE_WITH_ROOM_DETAIL = """
            {
                "CatalogOfferingsHospitalityResponse": {
                    "@type": "response",
                    "reservationStatus": "Success",
                    "CatalogOfferings": {
                        "@type": "CatalogOfferings",
                        "totalCatalogOffering": 1,
                        "catalogOfferingPerPage": 1,
                        "numberOfPages": 1,
                        "CatalogOffering": [
                            {
                                "@type": "CatalogOfferingHospitality",
                                "id": "f0c5e353-b50a-483b-9f3e-78c5333a6543:a9fac8d993a0d2d944d5fbed5a0f0790",
                                "Identifier": {
                                    "value": "f0c5e353-b50a-483b-9f3e-78c5333a6543:a9fac8d993a0d2d944d5fbed5a0f0790",
                                    "authority": "TVPT"
                                },
                                "ProductOptions": [
                                    {
                                        "@type": "ProductOptions",
                                        "Product": [
                                            {
                                                "@type": "ProductHospitality",
                                                "bookingCode": "A00AP7A",
                                                "propertyName": "Le Meridien New York Central Park",
                                                "PropertyAddress": {
                                                    "@type": "Address",
                                                    "AddressLine": ["120 West 57th Street"],
                                                    "City": "New York",
                                                    "StateProv": { "name": "NY" },
                                                    "Country": { "name": "US" },
                                                    "PostalCode": "10019"
                                                },
                                                "Telephone": { "@type": "Telephone", "phoneNumber": "+12128308000", "cityCode": "NYC" },
                                                "PropertyKey": { "@type": "PropertyKey", "chainCode": "MD", "propertyCode": "02892" },
                                                "RoomType": {
                                                    "@type": "RoomType",
                                                    "RoomCharacteristics": {
                                                        "@type": "RoomCharacteristics",
                                                        "typeCode": "75",
                                                        "smokingAllowed": "No",
                                                        "BedConfiguration": [
                                                            { "quantity": 1, "bedType": "King Bed(s)", "size": "180 cm X 200 cm" }
                                                        ],
                                                        "accessibleRoom": "No",
                                                        "RoomAmenity": [
                                                            { "@type": "RoomAmenity", "description": "REFRIGERATOR", "includedInd": true, "code": "88" }
                                                        ]
                                                    },
                                                    "Description": { "value": "Full, No Changes, Classic King, Guest Room, 1 King" }
                                                },
                                                "DateRange": { "start": "2026-08-29", "end": "2026-08-30" }
                                            }
                                        ]
                                    }
                                ],
                                "Price": {
                                    "@type": "PriceDetail",
                                    "CurrencyCode": { "value": "USD" },
                                    "Base": 407.15,
                                    "TotalTaxes": 115.20,
                                    "TotalPrice": 522.35
                                },
                                "TermsAndConditions": {
                                    "@type": "TermsAndConditionsHospitality",
                                    "Guarantee": [ { "@type": "Guarantee", "guaranteeType": "DepositRequired" } ],
                                    "CancelPenalty": [
                                        {
                                            "@type": "CancelPenalty",
                                            "Description": "If you cancel, modify, or do not arrive until Aug 29, 2026, you will be charged with a fee of 100% of the total stay.",
                                            "Deadline": { "@type": "Deadline", "SpecificDate": { "start": "2026-07-30" } },
                                            "HotelPenalty": { "@type": "HotelPenaltyPercent", "appliesTo": "Amount", "Percent": 100 },
                                            "Refundable": "No"
                                        }
                                    ],
                                    "Description": [
                                        "Prepay Non-Refundable Non-Changeable, Prepay In ",
                                        "Full, No Changes, Classic King, Guest Room, 1",
                                        "King"
                                    ],
                                    "RatePaymentInfo": "PrePay"
                                }
                            }
                        ]
                    },
                    "Result": { "@type": "Result", "status": "Complete" }
                }
            }
            """;

    @Test
    void deserializesRoomAndRateDetailFromRealTrace() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        TravelportHotelAvailabilityResponse response =
                mapper.readValue(SAMPLE_RESPONSE_WITH_ROOM_DETAIL, TravelportHotelAvailabilityResponse.class);

        var offering = response.CatalogOfferingsHospitalityResponse().CatalogOfferings().CatalogOffering().get(0);
        var product = offering.ProductOptions().get(0).Product().get(0);
        assertThat(product.bookingCode()).isEqualTo("A00AP7A");
        assertThat(product.PropertyAddress().City()).isEqualTo("New York");
        assertThat(product.Telephone().phoneNumber()).isEqualTo("+12128308000");
        assertThat(product.RoomType().Description().value()).isEqualTo("Full, No Changes, Classic King, Guest Room, 1 King");
        assertThat(product.RoomType().RoomCharacteristics().BedConfiguration().get(0).bedType()).isEqualTo("King Bed(s)");
        assertThat(product.RoomType().RoomCharacteristics().RoomAmenity().get(0).description()).isEqualTo("REFRIGERATOR");

        var terms = offering.TermsAndConditions();
        assertThat(terms.Guarantee().get(0).guaranteeType()).isEqualTo("DepositRequired");
        assertThat(terms.CancelPenalty().get(0).Refundable()).isEqualTo("No");
        assertThat(terms.CancelPenalty().get(0).HotelPenalty().Percent()).isEqualTo(100);
        assertThat(terms.CancelPenalty().get(0).Deadline().SpecificDate().start()).isEqualTo("2026-07-30");
        assertThat(terms.RatePaymentInfo()).isEqualTo("PrePay");
        assertThat(terms.Description()).hasSize(3);
    }

    @Test
    void serializesAnAvailabilityRequestByPropertyKey() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var guests = new TravelportHotelAvailabilityRequest.RoomStayCandidates(
                "RoomStayCandidates",
                List.of(new TravelportHotelAvailabilityRequest.RoomStayCandidate(
                        "RoomStayCandidate",
                        new TravelportHotelAvailabilityRequest.GuestCounts("GuestCounts", List.of(
                                new TravelportHotelAvailabilityRequest.GuestCount("GuestCount", 2))))));
        var request = new TravelportHotelAvailabilityRequest(
                new TravelportHotelAvailabilityRequest.CatalogOfferingsQueryRequest(
                        List.of(new TravelportHotelAvailabilityRequest.CatalogOfferingsRequest(
                                "CatalogOfferingsRequestHospitality",
                                true,
                                "EUR",
                                new TravelportHotelAvailabilityRequest.StayDates("2026-09-11", "2026-09-15"),
                                new TravelportHotelAvailabilityRequest.HotelSearchCriterion(
                                        "HotelSearchCriterion",
                                        1,
                                        List.of(new TravelportHotelAvailabilityRequest.PropertyRequest(
                                                "PropertyRequest",
                                                new TravelportHotelAvailabilityRequest.PropertyKey(
                                                        "PropertyKey", "UR", "G3375"))),
                                        guests)))));

        String json = mapper.writeValueAsString(request);

        assertThat(json).doesNotContain("\"CatalogOfferingsQueryRequest\":{\"@type\"");
        assertThat(json).contains("\"@type\":\"CatalogOfferingsRequestHospitality\"");
        assertThat(json).contains("\"verboseResponseInd\":true");
        assertThat(json).contains("\"chainCode\":\"UR\"");
        assertThat(json).contains("\"propertyCode\":\"G3375\"");
        assertThat(json).contains("\"start\":\"2026-09-11\"");
        assertThat(json).contains("\"numberOfRooms\":1");
        assertThat(json).contains("\"@type\":\"RoomStayCandidates\"");
        assertThat(json).contains("\"count\":2");
    }

    /**
     * Booking N rooms means N separate {@code RoomStayCandidate} entries in the list, one per
     * physical room - {@link TravelportClient#fetchHotelAvailability} builds this list from the
     * guest's room quantity so the availability re-check and reservation call both actually price
     * and reserve every room being paid for, instead of always just one.
     */
    @Test
    void serializesMultipleRoomStayCandidatesForMultiRoomBookings() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var roomStayCandidate = new TravelportHotelAvailabilityRequest.RoomStayCandidate(
                "RoomStayCandidate",
                new TravelportHotelAvailabilityRequest.GuestCounts("GuestCounts", List.of(
                        new TravelportHotelAvailabilityRequest.GuestCount("GuestCount", 1))));
        var guests = new TravelportHotelAvailabilityRequest.RoomStayCandidates(
                "RoomStayCandidates", List.of(roomStayCandidate, roomStayCandidate, roomStayCandidate));

        String json = mapper.writeValueAsString(guests);
        ObjectMapper reader = new ObjectMapper();
        var deserialized = reader.readValue(json, TravelportHotelAvailabilityRequest.RoomStayCandidates.class);

        assertThat(deserialized.RoomStayCandidate()).hasSize(3);
    }
}
