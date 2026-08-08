package com.guentours.usernotification;

import com.guentours.booking.BookingService;
import com.guentours.booking.domain.Booking;
import com.guentours.booking.event.BookingAutoCancelledEvent;
import com.guentours.booking.event.BookingFailedEvent;
import com.guentours.payment.events.PaymentFailedEvent;
import com.guentours.usernotification.domain.NotificationType;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Turns the domain events that leave a user "impacted" by something outside their control into
 * persisted, real-time in-app notifications. Separate from {@code notification.NotificationEventListener}
 * (which handles the email side of some of these same events) so the two concerns - transactional
 * email vs. in-app inbox - stay independently testable and one can't accidentally suppress the other.
 */
@Component
class UserNotificationEventListener {

    private final UserNotificationService notificationService;
    private final BookingService bookingService;

    UserNotificationEventListener(UserNotificationService notificationService, BookingService bookingService) {
        this.notificationService = notificationService;
        this.bookingService = bookingService;
    }

    @ApplicationModuleListener
    void on(BookingFailedEvent event) {
        Booking booking = bookingService.getById(event.bookingId());
        notificationService.create(booking.getUserId(), NotificationType.BOOKING_FAILED,
                "Booking failed",
                "We could not confirm your booking " + booking.getId() + ". Reason: " + booking.getFailureReason(),
                booking.getId());
    }

    @ApplicationModuleListener
    void on(BookingAutoCancelledEvent event) {
        Booking booking = bookingService.getById(event.bookingId());
        notificationService.create(booking.getUserId(), NotificationType.BOOKING_AUTO_CANCELLED,
                "Booking automatically cancelled",
                "Your booking " + booking.getId() + " was automatically cancelled. Reason: " + event.reason(),
                booking.getId());
    }

    @ApplicationModuleListener
    void on(PaymentFailedEvent event) {
        Booking booking = bookingService.getById(event.bookingId());
        notificationService.create(booking.getUserId(), NotificationType.PAYMENT_FAILED,
                "Payment failed",
                "The payment for your booking " + event.bookingId() + " failed. Reason: " + event.reason(),
                event.bookingId());
    }
}
