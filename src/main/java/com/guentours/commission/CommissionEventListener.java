package com.guentours.commission;

import com.guentours.booking.Booking;
import com.guentours.booking.BookingCreatedEvent;
import com.guentours.booking.BookingService;
import com.guentours.booking.OfferType;
import com.guentours.booking.ReservationFeePaidEvent;
import com.guentours.shared.CommissionPolicy;
import com.guentours.shared.Money;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Records the fixed booking fee into the commission wallet as soon as a booking is created.
 * Reacts to {@link BookingCreatedEvent} (fired synchronously at checkout time) instead of the
 * booking module depending directly on this one, so the dependency only ever points one way:
 * commission -&gt; booking, same as the ticketing/notification modules.
 */
@Component
class CommissionEventListener {

    private final BookingService bookingService;
    private final CommissionPolicy commissionPolicy;
    private final CommissionWalletService walletService;

    CommissionEventListener(BookingService bookingService, CommissionPolicy commissionPolicy,
                             CommissionWalletService walletService) {
        this.bookingService = bookingService;
        this.commissionPolicy = commissionPolicy;
        this.walletService = walletService;
    }

    @EventListener
    public void on(BookingCreatedEvent event) {
        Booking booking = bookingService.getById(event.bookingId());
        String currency = booking.getPrice().currency();

        Money totalFee;
        if (booking.getOfferType() == OfferType.HOTEL) {
            totalFee = commissionPolicy.hotelFee(currency);
        } else {
            int legs = booking.getItineraryLegs().isEmpty() ? 1 : booking.getItineraryLegs().size();
            Money feePerLeg = commissionPolicy.flightFee(currency);
            totalFee = Money.zero(currency);
            for (int i = 0; i < legs; i++) {
                totalFee = totalFee.add(feePerLeg);
            }
        }

        walletService.record(booking.getId(), booking.getProviderType(), booking.getOfferType(),
                CommissionType.BOOKING_FEE, totalFee);
    }

    /** Records the non-refundable PAY_LATER reservation fee as reservation commission once it is paid. */
    @EventListener
    public void on(ReservationFeePaidEvent event) {
        Booking booking = bookingService.getById(event.bookingId());
        Money fee = booking.getReservationFee();
        if (fee == null) {
            return;
        }
        walletService.record(booking.getId(), booking.getProviderType(), booking.getOfferType(),
                CommissionType.RESERVATION_FEE, fee);
    }
}
