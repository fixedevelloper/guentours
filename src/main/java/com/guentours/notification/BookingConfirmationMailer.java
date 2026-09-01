package com.guentours.notification;

import com.guentours.booking.BookingService;
import com.guentours.booking.domain.Booking;
import com.guentours.booking.domain.BookingStatus;
import com.guentours.shared.exception.BusinessException;
import com.guentours.storage.StorageService;
import com.guentours.ticketing.ETicket;
import com.guentours.ticketing.ETicketService;
import com.guentours.user.domain.User;
import com.guentours.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Builds and sends the "your booking is confirmed" email - the single source of truth for that
 * content, used both by {@link NotificationEventListener#on(com.guentours.booking.event.BookingConfirmedEvent)}
 * (automatic, on first confirmation) and by {@code AdminBookingNotificationController} (staff
 * "Envoyer Email" action, to resend the exact same email on demand). Public, unlike most classes
 * in this module, because that controller lives in the {@code notification.web} sub-package.
 */
@Service
public class BookingConfirmationMailer {

    private static final Logger log = LoggerFactory.getLogger(BookingConfirmationMailer.class);

    private final EmailService emailService;
    private final UserService userService;
    private final BookingService bookingService;
    private final ETicketService eTicketService;
    private final StorageService storageService;

    BookingConfirmationMailer(EmailService emailService, UserService userService, BookingService bookingService,
                              ETicketService eTicketService, StorageService storageService) {
        this.emailService = emailService;
        this.userService = userService;
        this.bookingService = bookingService;
        this.eTicketService = eTicketService;
        this.storageService = storageService;
    }

    /** Called for the automatic send right after the provider confirms the booking - status is
     *  trusted without checking since the event itself is the source of truth for that. */
    void send(String bookingId) {
        Booking booking = bookingService.getById(bookingId);
        sendFor(booking);
    }

    /** Staff-initiated resend (see {@code AdminBookingNotificationController}) - unlike the
     *  automatic send, the booking might not actually be confirmed (a stale/incorrect click), so
     *  this rejects rather than sending a confirmation email that contradicts the booking's real
     *  state. */
    public void resend(String bookingId) {
        Booking booking = bookingService.getById(bookingId);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException("Cannot resend a confirmation email for a booking that isn't CONFIRMED "
                    + "(current status: " + booking.getStatus() + ")");
        }
        sendFor(booking);
    }

    private void sendFor(Booking booking) {
        User user = userService.getById(booking.getUserId());
        String subject = "Confirmation de votre reservation Guens travel";
        String body = """
                Bonjour %s,

                Votre reservation est confirmee !

                Reference booking :      %s
                Code de confirmation :   %s
                Billets electroniques :  %s

                Merci de voyager avec Guens travel.
                """.formatted(user.getFullName(), booking.getId(), booking.getProviderConfirmationNumber(),
                String.join(", ", booking.getETicketNumbers()));

        byte[] ticketPdf = firstTicketPdf(booking.getId());
        if (ticketPdf != null) {
            emailService.sendWithAttachment(booking.getContactEmail(), subject, body, false, ticketPdf,
                    "billet.pdf", "application/pdf");
        } else {
            emailService.send(booking.getContactEmail(), subject, body);
        }
    }

    /**
     * Best-effort: the PDF is generated asynchronously by the ticketing module reacting to the
     * same event (see {@code ETicketService.on(BookingConfirmedEvent)}) and can legitimately be
     * unavailable (rendering failed, or this races ahead of it) - falls back to the plain-text
     * confirmation without an attachment rather than failing the whole notification.
     */
    private byte[] firstTicketPdf(String bookingId) {
        List<ETicket> tickets = eTicketService.getForBookingInternal(bookingId);
        if (tickets.isEmpty() || tickets.get(0).getPdfUrl() == null) {
            return null;
        }
        try {
            return storageService.download(tickets.get(0).getPdfUrl());
        } catch (Exception ex) {
            log.warn("Could not download ticket PDF for booking {}, sending confirmation without attachment: {}",
                    bookingId, ex.getMessage());
            return null;
        }
    }
}
