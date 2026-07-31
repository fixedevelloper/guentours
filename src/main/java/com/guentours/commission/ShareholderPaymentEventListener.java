package com.guentours.commission;

import com.guentours.payment.domain.Payment;
import com.guentours.payment.events.BookingDepositPaidEvent;
import com.guentours.payment.events.BookingFullyPaidEvent;
import com.guentours.payment.service.PaymentService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Splits every successful payment (deposit included, not just the final/full payment) across
 * active shareholders. Reacts to {@link BookingDepositPaidEvent} and {@link BookingFullyPaidEvent}
 * instead of the payment module depending directly on this one, so the dependency only ever
 * points one way: commission -&gt; payment, same as commission -&gt; booking.
 */
@Component
class ShareholderPaymentEventListener {

    private final PaymentService paymentService;
    private final ShareholderService shareholderService;

    ShareholderPaymentEventListener(PaymentService paymentService, ShareholderService shareholderService) {
        this.paymentService = paymentService;
        this.shareholderService = shareholderService;
    }

    @EventListener
    public void on(BookingDepositPaidEvent event) {
        split(event.paymentId());
    }

    @EventListener
    public void on(BookingFullyPaidEvent event) {
        split(event.paymentId());
    }

    private void split(String paymentId) {
        Payment payment = paymentService.getById(paymentId);
        shareholderService.recordPaymentSplit(payment.getId(), payment.getAmount());
    }
}
