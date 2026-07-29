package com.guentours.provider.travelopro;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the shape of Travelopro's {@code aeroVE5/extra_services} response used to build the real
 * seat map: {@code ExtraServicesData.DynamicSeat} is a per-sector array of decks/rows/seats, and
 * unmodeled ancillaries ({@code DynamicBaggage}/{@code DynamicMeal}) must be silently ignored
 * rather than fail deserialization.
 */
class TraveloproExtraServicesDtosTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializesTheSeatMapWireShapeAndIgnoresUnmodeledAncillaries() throws Exception {
        String json = """
                {
                  "success": true,
                  "ExtraServicesData": {
                    "DynamicBaggage": [ { "Behavior": "PER_PAX_OUTBOUND", "IsMultiSelect": true, "Services": [] } ],
                    "DynamicMeal": [ { "Behavior": "PER_PAX_OUTBOUND", "IsMultiSelect": true, "Services": [] } ],
                    "DynamicSeat": [
                      {
                        "Behavior": "PER_PAX_OUTBOUND",
                        "IsMultiSelect": true,
                        "DeckSeats": [
                          {
                            "DeckNo": 1,
                            "RowSeats": [
                              {
                                "RowNo": "1",
                                "Seats": [
                                  {
                                    "ServiceId": "MF8wXzFfMA",
                                    "AirlineCode": "6E",
                                    "FlightNumber": "1502",
                                    "EquipmentCode": "A320-186",
                                    "DepartureAirportLocationCode": "DXB",
                                    "ArrivalAirportLocationCode": "AMS",
                                    "DeckNo": "1",
                                    "RowNo": "1",
                                    "SeatNo": "A",
                                    "SeatCode": "1A",
                                    "AvailabilityType": { "Code": "3", "Text": "Reserved" },
                                    "Description": { "Code": "3", "Text": "The fare includes the seat" },
                                    "Compartment": { "Code": "1", "Text": "Compartment 1" },
                                    "SeatType": { "Code": "1", "Text": "Window" },
                                    "SeatWayType": { "Code": "1", "Text": "Segment" },
                                    "Fare": { "Amount": "10.23", "CurrencyCode": "USD", "DecimalPlaces": "2" }
                                  },
                                  {
                                    "ServiceId": "MF8wXzFfMQ",
                                    "RowNo": "1",
                                    "SeatNo": "B",
                                    "SeatCode": "1B",
                                    "AvailabilityType": { "Code": "1", "Text": "Open" },
                                    "Fare": { "Amount": "0", "CurrencyCode": "USD", "DecimalPlaces": "2" }
                                  }
                                ]
                              }
                            ]
                          }
                        ]
                      }
                    ]
                  }
                }
                """;

        TraveloproExtraServicesResponse response = mapper.readValue(json, TraveloproExtraServicesResponse.class);

        assertThat(response.success()).isTrue();
        var sector = response.ExtraServicesData().DynamicSeat().get(0);
        var deck = sector.DeckSeats().get(0);
        var row = deck.RowSeats().get(0);
        assertThat(row.RowNo()).isEqualTo("1");
        assertThat(row.Seats()).hasSize(2);

        var reservedSeat = row.Seats().get(0);
        assertThat(reservedSeat.SeatNo()).isEqualTo("A");
        assertThat(reservedSeat.AvailabilityType().Code()).isEqualTo("3");
        assertThat(reservedSeat.Fare().Amount()).isEqualTo("10.23");
        assertThat(reservedSeat.Fare().CurrencyCode()).isEqualTo("USD");

        var openSeat = row.Seats().get(1);
        assertThat(openSeat.SeatNo()).isEqualTo("B");
        assertThat(openSeat.AvailabilityType().Code()).isEqualTo("1");
    }

    @Test
    void serializesTheRequestWithSessionIdAndFareSourceCode() throws Exception {
        var request = new TraveloproExtraServicesRequest("sess-123", "FARE-XYZ");

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"session_id\":\"sess-123\"");
        assertThat(json).contains("\"fare_source_code\":\"FARE-XYZ\"");
    }
}
