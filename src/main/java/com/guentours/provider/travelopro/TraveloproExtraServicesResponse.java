package com.guentours.provider.travelopro;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Response for {@code POST /api/aeroVE5/extra_services}. Only the {@code DynamicSeat} branch is
 * modeled - {@code DynamicBaggage}/{@code DynamicMeal} ancillaries are outside the scope of the
 * seat-map integration and are simply ignored by {@code @JsonIgnoreProperties}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TraveloproExtraServicesResponse(Boolean success, ExtraServicesData ExtraServicesData) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExtraServicesData(List<DynamicSeatSector> DynamicSeat) {}

    /** One entry per journey sector (one-way = 1, round-trip = 2: outbound then return). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record DynamicSeatSector(String Behavior, Boolean IsMultiSelect, List<Deck> DeckSeats) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Deck(Integer DeckNo, List<RowSeats> RowSeats) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RowSeats(String RowNo, List<SeatEntry> Seats) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SeatEntry(
            String ServiceId,
            String AirlineCode,
            String FlightNumber,
            String EquipmentCode,
            String DepartureAirportLocationCode,
            String ArrivalAirportLocationCode,
            String DeckNo,
            String RowNo,
            String SeatNo,
            String SeatCode,
            CodeText AvailabilityType,
            CodeText Description,
            CodeText Compartment,
            CodeText SeatType,
            CodeText SeatWayType,
            Fare Fare
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CodeText(String Code, String Text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Fare(String Amount, String CurrencyCode, String DecimalPlaces) {}
}
