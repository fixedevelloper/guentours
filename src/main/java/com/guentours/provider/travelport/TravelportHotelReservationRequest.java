package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request body for Travelport's Stays Create Hotel Reservation (full payload) endpoint
 * ({@code POST /hotel/book/reservations}), matching a verified real sample. The body wraps a
 * {@code ReservationDetail} whose shape is the same {@code @type: Reservation} used by the flights
 * workbench - carrying the {@code Offer} (the cached availability offer), the {@code Traveler}(s),
 * and optionally {@code FormOfPayment}/{@code Payment} - so it reuses
 * {@link TravelportWorkbenchRequests.Reservation}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TravelportHotelReservationRequest(TravelportWorkbenchRequests.Reservation ReservationDetail) {
}
