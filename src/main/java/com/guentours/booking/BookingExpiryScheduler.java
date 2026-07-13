package com.guentours.booking;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Sweeps holds whose provider ticketing deadline lapsed without full payment and cancels them. */
@Component
class BookingExpiryScheduler {

    private final BookingService bookingService;

    BookingExpiryScheduler(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedRateString = "${app.booking.expiry-check-interval-ms:300000}")
    void cancelExpiredHolds() {
        bookingService.cancelExpiredHolds();
    }
}
