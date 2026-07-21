package com.guentours.provider.travelopro;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the shape of Travelopro/TravelNext's Hotel API v6 request/response contracts against the
 * fields the vendor's reference PHP client reads/writes: {@code hotel_search} returns
 * {@code status} + {@code itineraries} (each hotel carries {@code productId}/{@code tokenId}),
 * {@code get_room_rates} returns {@code roomRates.perBookingRates} (each with a {@code rateBasisId}),
 * and {@code hotel_book} comes back {@code CONFIRMED} with a {@code referenceNum}.
 */
class TraveloproHotelDtosTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializesTheHotelSearchWireShape() throws Exception {
        String json = """
                {
                  "status": {
                    "sessionId": "sess-123",
                    "moreResults": true,
                    "nextToken": "tok-2",
                    "totalResults": 42
                  },
                  "itineraries": [
                    {
                      "hotelId": "H-9001",
                      "twxHotelId": "TWX-9001",
                      "productId": "P-77",
                      "tokenId": "T-88",
                      "hotelName": "Ibis Central",
                      "hotelRating": "4",
                      "propertyType": "Hotels",
                      "fareType": "Refundable",
                      "total": 213.50,
                      "currency": "EUR",
                      "city": "Paris",
                      "country": "France",
                      "latitude": "48.85",
                      "longitude": "2.35",
                      "facilities": ["WiFi", "Parking"]
                    }
                  ]
                }
                """;

        TraveloproHotelSearchResponse response = mapper.readValue(json, TraveloproHotelSearchResponse.class);

        assertThat(response.status().sessionId()).isEqualTo("sess-123");
        assertThat(response.status().moreResults()).isTrue();
        assertThat(response.itineraries()).hasSize(1);
        var hotel = response.itineraries().get(0);
        assertThat(hotel.hotelId()).isEqualTo("H-9001");
        assertThat(hotel.productId()).isEqualTo("P-77");
        assertThat(hotel.tokenId()).isEqualTo("T-88");
        assertThat(hotel.hotelName()).isEqualTo("Ibis Central");
        assertThat(hotel.total()).isEqualTo(213.50);
        assertThat(hotel.currency()).isEqualTo("EUR");
    }

    @Test
    void deserializesAValidationError() throws Exception {
        String json = """
                { "Errors": { "ErrorCode": "1001", "ErrorMessage": "Invalid city" } }
                """;

        TraveloproHotelSearchResponse response = mapper.readValue(json, TraveloproHotelSearchResponse.class);

        assertThat(response.itineraries()).isNull();
        assertThat(response.Errors().ErrorCode()).isEqualTo("1001");
        assertThat(response.Errors().ErrorMessage()).isEqualTo("Invalid city");
    }

    @Test
    void deserializesTheRoomRatesWireShape() throws Exception {
        String json = """
                {
                  "sessionId": "sess-123",
                  "hotelId": "H-9001",
                  "tokenId": "T-88",
                  "roomRates": {
                    "perBookingRates": [
                      {
                        "productId": "P-77",
                        "roomType": "Double",
                        "rateBasisId": "RB-1",
                        "currency": "EUR",
                        "netPrice": 205.00,
                        "boardType": "Room Only",
                        "maxOccupancyPerRoom": 2,
                        "cancellationPolicy": "Free until 2026-09-01|t|50% after"
                      }
                    ]
                  }
                }
                """;

        TraveloproRoomRatesResponse response = mapper.readValue(json, TraveloproRoomRatesResponse.class);

        var rates = response.roomRates().perBookingRates();
        assertThat(rates).hasSize(1);
        assertThat(rates.get(0).rateBasisId()).isEqualTo("RB-1");
        assertThat(rates.get(0).netPrice()).isEqualTo(205.00);
        assertThat(rates.get(0).cancellationPolicy()).contains("|t|");
    }

    @Test
    void deserializesAConfirmedBooking() throws Exception {
        String json = """
                {
                  "status": "CONFIRMED",
                  "supplierConfirmationNum": "SUP-555",
                  "referenceNum": "REF-999",
                  "clientRefNum": "GT-abc",
                  "productId": "P-77",
                  "roomBookDetails": {
                    "hotelId": "H-9001",
                    "checkIn": "2026-09-11",
                    "checkOut": "2026-09-15",
                    "days": "4",
                    "currency": "EUR",
                    "NetPrice": "205.00",
                    "fareType": "Refundable"
                  }
                }
                """;

        TraveloproHotelBookResponse response = mapper.readValue(json, TraveloproHotelBookResponse.class);

        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(response.referenceNum()).isEqualTo("REF-999");
        assertThat(response.supplierConfirmationNum()).isEqualTo("SUP-555");
        assertThat(response.roomBookDetails().hotelId()).isEqualTo("H-9001");
    }

    @Test
    void serializesTheSearchRequestWithSnakeCaseAuthAndOccupancy() throws Exception {
        var request = new TraveloproHotelSearchRequest(
                "uid", "pwd", "Test", "127.0.0.1",
                "EUR", "US", "2026-09-11", "2026-09-15",
                "Paris", null, 20, 30,
                List.of(new TraveloproHotelSearchRequest.Occupancy(1, 2, 0, List.of())));

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"user_id\":\"uid\"");
        assertThat(json).contains("\"user_password\":\"pwd\"");
        assertThat(json).contains("\"requiredCurrency\":\"EUR\"");
        assertThat(json).contains("\"city_name\":\"Paris\"");
        assertThat(json).contains("\"room_no\":1");
        assertThat(json).contains("\"adult\":2");
        // country_name is null and must be omitted, not serialized as null.
        assertThat(json).doesNotContain("country_name");
    }

    @Test
    void serializesTheBookRequestWithColumnOrientedPaxNames() throws Exception {
        var request = new TraveloproHotelBookRequest(
                "uid", "pwd", "Test", "127.0.0.1",
                "sess-123", "P-77", "T-88", "RB-1", "GT-abc",
                "jane@example.com", null, null,
                List.of(new TraveloproHotelBookRequest.PaxDetail(1,
                        new TraveloproHotelBookRequest.PaxNames(List.of("Mr"), List.of("Jane"), List.of("Traveler")),
                        null)));

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"rateBasisId\":\"RB-1\"");
        assertThat(json).contains("\"paxDetails\"");
        assertThat(json).contains("\"firstName\":[\"Jane\"]");
        assertThat(json).contains("\"lastName\":[\"Traveler\"]");
        // customerPhone is null and must be omitted.
        assertThat(json).doesNotContain("customerPhone");
    }
}
