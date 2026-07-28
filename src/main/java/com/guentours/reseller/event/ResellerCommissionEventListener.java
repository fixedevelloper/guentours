package com.guentours.reseller.event;

import com.guentours.payment.events.BookingFullyPaidEvent;
import com.guentours.reseller.service.ResellerCommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class ResellerCommissionEventListener {

    private final ResellerCommissionService resellerCommissionService;

    @ApplicationModuleListener
    void on(BookingFullyPaidEvent event) {
        log.debug("BookingFullyPaidEvent reçu pour le booking {}", event.bookingId());
        resellerCommissionService.handleBookingPaymentConfirmed(event.bookingId());
    }
}