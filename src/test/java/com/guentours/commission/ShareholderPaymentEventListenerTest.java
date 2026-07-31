package com.guentours.commission;

import com.guentours.payment.domain.Payment;
import com.guentours.payment.events.BookingDepositPaidEvent;
import com.guentours.payment.events.BookingFullyPaidEvent;
import com.guentours.payment.service.PaymentService;
import com.guentours.shared.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifies the shareholder split fires on both a deposit and a full payment, from the payment's own amount. */
@ExtendWith(MockitoExtension.class)
class ShareholderPaymentEventListenerTest {

    @Mock
    private PaymentService paymentService;
    @Mock
    private ShareholderService shareholderService;
    @Mock
    private Payment payment;

    private ShareholderPaymentEventListener listener;

    @Test
    void splitsOnADepositPayment() {
        listener = new ShareholderPaymentEventListener(paymentService, shareholderService);
        when(paymentService.getById("payment-1")).thenReturn(payment);
        when(payment.getId()).thenReturn("payment-1");
        Money amount = new Money(BigDecimal.valueOf(50), "EUR");
        when(payment.getAmount()).thenReturn(amount);

        listener.on(new BookingDepositPaidEvent("booking-1", "payment-1", "gw-ref"));

        verify(shareholderService).recordPaymentSplit("payment-1", amount);
    }

    @Test
    void splitsOnAFullPayment() {
        listener = new ShareholderPaymentEventListener(paymentService, shareholderService);
        when(paymentService.getById("payment-2")).thenReturn(payment);
        when(payment.getId()).thenReturn("payment-2");
        Money amount = new Money(BigDecimal.valueOf(150), "EUR");
        when(payment.getAmount()).thenReturn(amount);

        listener.on(new BookingFullyPaidEvent("booking-2", "payment-2", "gw-ref", "1234"));

        verify(shareholderService).recordPaymentSplit("payment-2", amount);
    }
}
