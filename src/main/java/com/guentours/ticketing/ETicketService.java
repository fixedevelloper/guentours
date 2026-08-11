package com.guentours.ticketing;

import com.guentours.booking.domain.Booking;
import com.guentours.booking.event.BookingConfirmedEvent;
import com.guentours.booking.BookingService;
import com.guentours.shared.exception.BusinessException;
import com.guentours.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ETicketService {

    private static final Logger log = LoggerFactory.getLogger(ETicketService.class);

    private final ETicketRepository eTicketRepository;
    private final BookingService bookingService;
    private final TicketDocumentService ticketDocumentService;
    private final TicketEmailService ticketEmailService;

    public ETicketService(ETicketRepository eTicketRepository, BookingService bookingService,
                          TicketDocumentService ticketDocumentService, TicketEmailService ticketEmailService) {
        this.eTicketRepository = eTicketRepository;
        this.bookingService = bookingService;
        this.ticketDocumentService = ticketDocumentService;
        this.ticketEmailService = ticketEmailService;
    }

    @ApplicationModuleListener
    void on(BookingConfirmedEvent event) {
        Booking booking = bookingService.getById(event.bookingId());
        for (String ticketNumber : booking.getETicketNumbers()) {
            String document = renderDocument(booking, ticketNumber);
            ETicket ticket = new ETicket(booking.getId(), ticketNumber, booking.getProviderConfirmationNumber(), document);
            ticket.setPdfUrl(renderAndUploadPdf(booking, ticketNumber));
            eTicketRepository.save(ticket);
        }
        log.info("Generated {} e-ticket(s) for booking {}", booking.getETicketNumbers().size(), booking.getId());
    }

    /**
     * Never lets a PDF rendering/upload failure (bad template data, MinIO unreachable, ...) block
     * ticket creation - the plain-text {@code document} on {@link ETicket} always exists as a
     * fallback, so a null {@code pdfUrl} here just means no branded PDF is available yet rather
     * than losing the ticket entirely.
     */
    private String renderAndUploadPdf(Booking booking, String ticketNumber) {
        try {
            byte[] pdf = ticketDocumentService.renderPdf(booking, ticketNumber);
            return ticketDocumentService.uploadPdf(booking.getId(), ticketNumber, pdf);
        } catch (Exception ex) {
            log.error("Failed to generate PDF for ticket {} (booking {}): {}", ticketNumber, booking.getId(), ex.getMessage());
            return null;
        }
    }

    public List<ETicket> getForBooking(String bookingId, String email) {
        Booking booking = bookingService.getById(bookingId);
        bookingService.verifyGuestAccess(booking, email);
        return eTicketRepository.findByBookingId(bookingId);
    }

    /**
     * Same data as {@link #getForBooking}, without the guest-access check - reserved for trusted
     * internal callers (e.g. {@code notification}, attaching the PDF to the automatic
     * booking-confirmed email) that already act on the booking's own behalf, never for anything
     * reachable from an external request.
     */
    public List<ETicket> getForBookingInternal(String bookingId) {
        return eTicketRepository.findByBookingId(bookingId);
    }

    /**
     * Shares a ticket's PDF by email at the requester's own request - {@code requesterEmail} must
     * match the booking's contact email (same {@link com.guentours.booking.BookingService#verifyGuestAccess}
     * check as {@link #getForBooking}), so a stranger who merely guesses a ticket id can't trigger
     * a send or exfiltrate the PDF. {@code recipientEmail} lets the requester share with someone
     * else (e.g. a travel companion); defaults to their own contact email when blank.
     */
    public void sendByEmail(String ticketId, String requesterEmail, String recipientEmail) {
        ETicket ticket = eTicketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket not found: " + ticketId));
        Booking booking = bookingService.getById(ticket.getBookingId());
        bookingService.verifyGuestAccess(booking, requesterEmail);

        if (ticket.getPdfUrl() == null) {
            throw new BusinessException("No PDF is available yet for this ticket, please try again shortly");
        }

        String to = (recipientEmail != null && !recipientEmail.isBlank()) ? recipientEmail : booking.getContactEmail();
        ticketEmailService.sendTicketByEmail(ticket, to);
    }

    private String renderDocument(Booking booking, String ticketNumber) {
        return """
                =====================================
                        ELECTRONIC TICKET
                =====================================
                Ticket number:      %s
                Confirmation code:  %s
                Passenger contact:  %s
                Booking reference:  %s
                Type:                %s
                =====================================
                """.formatted(ticketNumber, booking.getProviderConfirmationNumber(), booking.getContactEmail(),
                booking.getId(), booking.getOfferType());
    }
}
