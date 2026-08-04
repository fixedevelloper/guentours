package com.guentours.provider;

import java.time.LocalDateTime;

/** Result of {@link TravelProviderClient#createFlightHold}/{@code createHotelHold} - a PNR/reservation
 *  reference held with the provider, plus the deadline by which it must be ticketed/finalized or it lapses.
 *  {@code supplierLocator} is Travelport hotel-only (needed later by its cancellation call alongside
 *  {@code pnrCode}); null for every other provider/offer type. */
public record ProviderBookingConfirmation(
        ProviderType providerType,
        String pnrCode,
        LocalDateTime ticketingDeadline,
        boolean confirmed,
        String supplierLocator
) {
    public ProviderBookingConfirmation(ProviderType providerType, String pnrCode,
                                        LocalDateTime ticketingDeadline, boolean confirmed) {
        this(providerType, pnrCode, ticketingDeadline, confirmed, null);
    }
}
