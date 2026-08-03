package com.guentours.commission;

import com.guentours.booking.domain.Booking;
import com.guentours.booking.event.BookingCreatedEvent;
import com.guentours.booking.BookingService;
import com.guentours.booking.event.ReservationFeePaidEvent;
import com.guentours.shared.CommissionPolicy;
import com.guentours.shared.Money;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Records the percentage-based booking fee into the commission wallet as soon as a booking is
 * created. Reacts to {@link BookingCreatedEvent} (fired synchronously at checkout time) instead of
 * the booking module depending directly on this one, so the dependency only ever points one way:
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

        // The fee is a percentage of the provider's price, already folded into booking.getPrice()
        // by whichever CommissionPolicy.addXFee call built this booking - so it's recovered from
        // that final total rather than recomputed from a leg/room count.
        Money totalFee = switch (booking.getOfferType()) {
            case FLIGHT -> commissionPolicy.flightFeeFromTotal(booking.getPrice());
            case HOTEL -> commissionPolicy.hotelFeeFromTotal(booking.getPrice());
            case CAR_RENTAL -> commissionPolicy.vehicleFeeFromTotal(booking.getPrice());
            case FURNISHED_RENTAL -> commissionPolicy.propertyFeeFromTotal(booking.getPrice());
        };

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
