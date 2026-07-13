package com.guentours.booking;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Drives provider confirmation on a background thread once a booking's PAID
 * status has actually been committed - decoupled from {@code markPaidAndConfirm}
 * via an event so the (potentially slow) provider call never runs inside, or
 * blocks, the payment request's transaction.
 */
@Component
class BookingConfirmationListener {

    private final BookingService bookingService;

    BookingConfirmationListener(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(BookingPaidEvent event) {
        bookingService.confirmWithProvider(event.bookingId(), event.paymentTransactionReference(), event.cardLast4());
    }
}
