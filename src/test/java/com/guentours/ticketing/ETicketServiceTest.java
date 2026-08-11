package com.guentours.ticketing;

import com.guentours.booking.BookingService;
import com.guentours.booking.domain.Booking;
import com.guentours.booking.event.BookingConfirmedEvent;
import com.guentours.provider.ProviderType;
import com.guentours.shared.Money;
import com.guentours.shared.exception.BusinessException;
import com.guentours.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ETicketServiceTest {

    @Mock
    private ETicketRepository eTicketRepository;
    @Mock
    private BookingService bookingService;
    @Mock
    private TicketDocumentService ticketDocumentService;
    @Mock
    private TicketEmailService ticketEmailService;

    private ETicketService eTicketService;

    @BeforeEach
    void setUp() {
        eTicketService = new ETicketService(eTicketRepository, bookingService, ticketDocumentService, ticketEmailService);
    }

    private Booking confirmedFlightBooking(List<String> eTicketNumbers) {
        Booking booking = Booking.forFlight("user-1", "jean@example.com", ProviderType.DIRECT, "offer-1",
                "AF", "AF123", "CDG", "JFK", LocalDateTime.now().plusDays(10),
                LocalDateTime.now().plusDays(10).plusHours(9), "ECONOMY", Money.of(150_000, "XAF"), List.of());
        booking.markConfirmed("PNR123", eTicketNumbers);
        return booking;
    }

    @Test
    @DisplayName("on(BookingConfirmedEvent) saves one ETicket per e-ticket number, each with a rendered document and PDF URL")
    void generatesOneETicketPerNumber() {
        Booking booking = confirmedFlightBooking(List.of("ET-001", "ET-002"));
        when(bookingService.getById("booking-1")).thenReturn(booking);
        when(ticketDocumentService.renderPdf(eq(booking), anyString())).thenReturn(new byte[] {1, 2, 3});
        when(ticketDocumentService.uploadPdf(eq(booking.getId()), anyString(), any(byte[].class)))
                .thenAnswer(inv -> "https://minio.example/sandbox/tickets/" + booking.getId() + "/" + inv.getArgument(1) + ".pdf");

        eTicketService.on(new BookingConfirmedEvent("booking-1"));

        ArgumentCaptor<ETicket> savedCaptor = ArgumentCaptor.forClass(ETicket.class);
        verify(eTicketRepository, times(2)).save(savedCaptor.capture());

        List<ETicket> saved = savedCaptor.getAllValues();
        assertThat(saved).extracting(ETicket::getTicketNumber).containsExactly("ET-001", "ET-002");
        assertThat(saved).allSatisfy(ticket -> {
            assertThat(ticket.getBookingId()).isEqualTo(booking.getId());
            assertThat(ticket.getProviderConfirmationNumber()).isEqualTo("PNR123");
            assertThat(ticket.getDocument()).contains("PNR123", "jean@example.com", "FLIGHT");
            assertThat(ticket.getPdfUrl()).endsWith(ticket.getTicketNumber() + ".pdf");
        });
    }

    @Test
    @DisplayName("on(BookingConfirmedEvent) still saves the ticket (with a null pdfUrl) when PDF rendering fails")
    void savesTicketWithoutPdfUrlWhenPdfRenderingFails() {
        Booking booking = confirmedFlightBooking(List.of("ET-001"));
        when(bookingService.getById("booking-1")).thenReturn(booking);
        when(ticketDocumentService.renderPdf(eq(booking), anyString())).thenThrow(new RuntimeException("boom"));

        eTicketService.on(new BookingConfirmedEvent("booking-1"));

        ArgumentCaptor<ETicket> savedCaptor = ArgumentCaptor.forClass(ETicket.class);
        verify(eTicketRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getPdfUrl()).isNull();
        assertThat(savedCaptor.getValue().getDocument()).contains("PNR123");
        verify(ticketDocumentService, never()).uploadPdf(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("on(BookingConfirmedEvent) saves nothing for a booking with no e-ticket numbers")
    void savesNothingWhenNoETicketNumbers() {
        Booking booking = confirmedFlightBooking(List.of());
        when(bookingService.getById("booking-2")).thenReturn(booking);

        eTicketService.on(new BookingConfirmedEvent("booking-2"));

        verifyNoInteractions(eTicketRepository);
    }

    @Test
    @DisplayName("getForBooking returns the repository's tickets once guest access is verified")
    void getForBookingReturnsTicketsAfterAccessCheck() {
        Booking booking = confirmedFlightBooking(List.of("ET-001"));
        when(bookingService.getById("booking-1")).thenReturn(booking);
        List<ETicket> tickets = List.of(new ETicket("booking-1", "ET-001", "PNR123", "doc"));
        when(eTicketRepository.findByBookingId("booking-1")).thenReturn(tickets);

        List<ETicket> result = eTicketService.getForBooking("booking-1", "jean@example.com");

        assertThat(result).isEqualTo(tickets);
        verify(bookingService).verifyGuestAccess(booking, "jean@example.com");
    }

    @Test
    @DisplayName("getForBooking propagates the access-denied failure instead of leaking another guest's tickets")
    void getForBookingPropagatesAccessDenied() {
        Booking booking = confirmedFlightBooking(List.of("ET-001"));
        when(bookingService.getById("booking-1")).thenReturn(booking);
        doThrow(new NotFoundException("Booking not found: booking-1"))
                .when(bookingService).verifyGuestAccess(booking, "wrong@example.com");

        assertThatThrownBy(() -> eTicketService.getForBooking("booking-1", "wrong@example.com"))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(eTicketRepository);
    }

    @Test
    @DisplayName("getForBookingInternal returns the repository's tickets without any access check")
    void getForBookingInternalSkipsAccessCheck() {
        List<ETicket> tickets = List.of(new ETicket("booking-1", "ET-001", "PNR123", "doc"));
        when(eTicketRepository.findByBookingId("booking-1")).thenReturn(tickets);

        List<ETicket> result = eTicketService.getForBookingInternal("booking-1");

        assertThat(result).isEqualTo(tickets);
        verifyNoInteractions(bookingService);
    }

    private ETicket ticketWithPdf(String bookingId, String ticketNumber, String pdfUrl) {
        ETicket ticket = new ETicket(bookingId, ticketNumber, "PNR123", "doc");
        ticket.setPdfUrl(pdfUrl);
        return ticket;
    }

    @Test
    @DisplayName("sendByEmail defaults the recipient to the booking's own contact email")
    void sendByEmailDefaultsRecipientToContactEmail() {
        Booking booking = confirmedFlightBooking(List.of("ET-001"));
        ETicket ticket = ticketWithPdf("booking-1", "ET-001", "https://minio.example/sandbox/tickets/booking-1/ET-001.pdf");
        when(eTicketRepository.findById("ticket-1")).thenReturn(java.util.Optional.of(ticket));
        when(bookingService.getById("booking-1")).thenReturn(booking);

        eTicketService.sendByEmail("ticket-1", "jean@example.com", null);

        verify(bookingService).verifyGuestAccess(booking, "jean@example.com");
        verify(ticketEmailService).sendTicketByEmail(ticket, "jean@example.com");
    }

    @Test
    @DisplayName("sendByEmail sends to an explicit recipient when one is given")
    void sendByEmailUsesExplicitRecipient() {
        Booking booking = confirmedFlightBooking(List.of("ET-001"));
        ETicket ticket = ticketWithPdf("booking-1", "ET-001", "https://minio.example/sandbox/tickets/booking-1/ET-001.pdf");
        when(eTicketRepository.findById("ticket-1")).thenReturn(java.util.Optional.of(ticket));
        when(bookingService.getById("booking-1")).thenReturn(booking);

        eTicketService.sendByEmail("ticket-1", "jean@example.com", "friend@example.com");

        verify(ticketEmailService).sendTicketByEmail(ticket, "friend@example.com");
    }

    @Test
    @DisplayName("sendByEmail propagates access-denied instead of sending to/for a stranger")
    void sendByEmailPropagatesAccessDenied() {
        Booking booking = confirmedFlightBooking(List.of("ET-001"));
        ETicket ticket = ticketWithPdf("booking-1", "ET-001", "https://minio.example/sandbox/tickets/booking-1/ET-001.pdf");
        when(eTicketRepository.findById("ticket-1")).thenReturn(java.util.Optional.of(ticket));
        when(bookingService.getById("booking-1")).thenReturn(booking);
        doThrow(new NotFoundException("Booking not found: booking-1"))
                .when(bookingService).verifyGuestAccess(booking, "wrong@example.com");

        assertThatThrownBy(() -> eTicketService.sendByEmail("ticket-1", "wrong@example.com", null))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(ticketEmailService);
    }

    @Test
    @DisplayName("sendByEmail refuses when no PDF has been generated for this ticket yet")
    void sendByEmailRejectsWhenNoPdfAvailable() {
        Booking booking = confirmedFlightBooking(List.of("ET-001"));
        ETicket ticket = new ETicket("booking-1", "ET-001", "PNR123", "doc"); // pdfUrl left null
        when(eTicketRepository.findById("ticket-1")).thenReturn(java.util.Optional.of(ticket));
        when(bookingService.getById("booking-1")).thenReturn(booking);

        assertThatThrownBy(() -> eTicketService.sendByEmail("ticket-1", "jean@example.com", null))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(ticketEmailService);
    }
}
