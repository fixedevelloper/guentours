package com.guentours.provider;

import java.time.LocalDateTime;

/** Result of {@link TravelProviderClient#createFlightHold}/{@code createHotelHold} - a PNR/reservation
 *  reference held with the provider, plus the deadline by which it must be ticketed/finalized or it lapses. */
public record ProviderBookingConfirmation(
        ProviderType providerType,
        String pnrCode,
        LocalDateTime ticketingDeadline,
        boolean confirmed
) {
}
