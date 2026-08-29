package com.guentours.provider;

import java.util.List;

/**
 * Live, provider-agnostic view of a confirmed flight's supplementary details - baggage/meals/seats
 * per traveler and the cancellation policy - fetched on demand for the booking detail page (see
 * {@link TravelProviderClient#getFlightOrderDetail}). Route/times/passenger names already live on
 * {@code Booking} itself and aren't duplicated here.
 */
public record FlightOrderDetail(
        String bookingStatus,
        List<Traveler> travelers,
        List<CancellationRule> cancellationRules
) {
    public record Traveler(
            String firstName,
            String lastName,
            String paxType,
            List<Baggage> baggages,
            List<Meal> meals,
            List<Seat> seats
    ) {
    }

    public record Baggage(
            String segmentId,
            String departure,
            String arrival,
            String description,
            String price,
            String currency
    ) {
    }

    public record Meal(
            String segmentId,
            String departure,
            String arrival,
            String description,
            String price,
            String currency
    ) {
    }

    public record Seat(
            String segmentId,
            String departure,
            String arrival,
            String row,
            String column,
            String price,
            String currency
    ) {
    }

    public record CancellationRule(
            String adultCharges,
            String currency,
            boolean refundable
    ) {
    }
}
