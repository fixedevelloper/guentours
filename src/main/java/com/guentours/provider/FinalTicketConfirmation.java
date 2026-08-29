package com.guentours.provider;

import java.util.List;

/** Result of {@link TravelProviderClient#issueFlightTicket} - converts a held PNR into issued e-tickets.
 *  {@code reason} is null when {@code issued} is true; when false, it carries the provider's own
 *  explanation (e.g. a rejected-booking message) so BookingService can put the real cause in the
 *  booking's failureReason instead of a generic "provider declined" message. */
public record FinalTicketConfirmation(
        ProviderType providerType,
        String pnrCode,
        List<String> eTicketNumbers,
        boolean issued,
        String reason
) {
    public FinalTicketConfirmation(ProviderType providerType, String pnrCode, List<String> eTicketNumbers, boolean issued) {
        this(providerType, pnrCode, eTicketNumbers, issued, null);
    }
}
