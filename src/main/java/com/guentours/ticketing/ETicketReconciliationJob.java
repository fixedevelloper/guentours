package com.guentours.ticketing;

import com.guentours.booking.BookingService;
import com.guentours.booking.domain.Booking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Backfills e-ticket numbers for confirmed flight bookings whose provider ticketed
 * asynchronously and returned none yet at confirmation time. Travel Terminus is the only adapter
 * this applies to today - its own reconciliation runs roughly every 10 minutes on their side (see
 * {@code TravelTerminusClient.checkForIssuedTickets}) - so this job runs on the same cadence to
 * catch a booking within one cycle of the provider actually ticketing it, without hammering their
 * order-details endpoint. The actual provider lookup lives in {@link
 * BookingService#reconcileMissingETicketNumbers()} - {@code ticketing} isn't allowed to depend on
 * {@code provider} directly (see ModularityTests).
 */
@Slf4j
@Component
public class ETicketReconciliationJob {

    private static final long TEN_MINUTES_MS = 10 * 60_000L;

    private final BookingService bookingService;
    private final ETicketService eTicketService;

    public ETicketReconciliationJob(BookingService bookingService, ETicketService eTicketService) {
        this.bookingService = bookingService;
        this.eTicketService = eTicketService;
    }

    @Scheduled(fixedRate = TEN_MINUTES_MS, initialDelay = TEN_MINUTES_MS)
    public void reconcile() {
        List<Booking> updated = bookingService.reconcileMissingETicketNumbers();
        if (updated.isEmpty()) {
            return;
        }
        log.info("ETicketReconciliationJob: {} booking(s) got e-ticket numbers this cycle", updated.size());
        for (Booking booking : updated) {
            eTicketService.generateTicketsFor(booking);
        }
    }
}
