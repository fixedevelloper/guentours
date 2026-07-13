package com.guentours.provider;

/** Result of {@link TravelProviderClient#confirmHotelBooking} - finalizes a held hotel reservation. */
public record FinalHotelConfirmation(
        ProviderType providerType,
        String hotelBookingRef,
        String confirmationNumber,
        boolean confirmed
) {
}
