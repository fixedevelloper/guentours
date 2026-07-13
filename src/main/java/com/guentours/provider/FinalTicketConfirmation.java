package com.guentours.provider;

import java.util.List;

/** Result of {@link TravelProviderClient#issueFlightTicket} - converts a held PNR into issued e-tickets. */
public record FinalTicketConfirmation(
        ProviderType providerType,
        String pnrCode,
        List<String> eTicketNumbers,
        boolean issued
) {
}
