package com.guentours.ticketing;

import com.guentours.booking.domain.Booking;
import com.guentours.provider.ProviderType;
import com.guentours.reseller.domain.Reseller;
import com.guentours.reseller.domain.ResellerRepository;
import com.guentours.shared.Money;
import com.guentours.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Uses a real Thymeleaf TemplateEngine (same classpath template resolution Spring Boot's
 * autoconfiguration would set up) and a real openhtmltopdf render, so this actually exercises the
 * template syntax and PDF pipeline rather than mocking around them - only the repository/storage
 * edges (real DB, real MinIO) are mocked.
 */
@ExtendWith(MockitoExtension.class)
class TicketDocumentServiceTest {

    @Mock
    private ResellerRepository resellerRepository;
    @Mock
    private StorageService storageService;

    private TicketDocumentService ticketDocumentService;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        // SpringTemplateEngine (SpringStandardDialect/SpringEL), not the plain TemplateEngine
        // (StandardDialect/OGNL) - matches what Spring Boot's thymeleaf autoconfiguration actually
        // wires up as the TemplateEngine bean in production, and avoids pulling in OGNL as a test-only
        // dependency the app doesn't otherwise need.
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        ticketDocumentService = new TicketDocumentService(templateEngine, resellerRepository, storageService);
    }

    private Booking directBooking() {
        return Booking.forFlight("user-1", "jean@example.com", ProviderType.DIRECT, "offer-1",
                "AF", "AF123", "CDG", "JFK", LocalDateTime.now().plusDays(10),
                LocalDateTime.now().plusDays(10).plusHours(9), "ECONOMY", Money.of(150_000, "XAF"), List.of());
    }

    @Test
    @DisplayName("renders a real PDF for a direct booking (no reseller)")
    void rendersPdfForDirectBooking() {
        Booking booking = directBooking();
        booking.markConfirmed("PNR123", List.of("ET-001"));

        byte[] pdf = ticketDocumentService.renderPdf(booking, "ET-001");

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        verifyNoInteractions(resellerRepository);
    }

    @Test
    @DisplayName("renders a real PDF including the reseller's branding when the booking went through one")
    void rendersPdfForResellerBooking() {
        Booking booking = directBooking();
        booking.markConfirmed("PNR123", List.of("ET-001"));
        booking.assignReseller("reseller-1");

        Reseller reseller = new Reseller(null, "Acme Travel", "Jane Reseller", "jane@acme-travel.example",
                "699000000", "12345", "Douala", "Cameroun", "PROMO1", null,
                "https://minio.example/sandbox/resellers/logos/acme.png");
        when(resellerRepository.findById("reseller-1")).thenReturn(Optional.of(reseller));

        byte[] pdf = ticketDocumentService.renderPdf(booking, "ET-001");

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        verify(resellerRepository).findById("reseller-1");
    }

    @Test
    @DisplayName("uploadPdf delegates to StorageService with a booking/ticket-scoped key")
    void uploadPdfDelegatesToStorageService() {
        byte[] bytes = {1, 2, 3};
        when(storageService.upload(eq("tickets/booking-1"), any(byte[].class), eq("application/pdf"), eq("ET-001.pdf")))
                .thenReturn("https://minio.example/sandbox/tickets/booking-1/ET-001.pdf");

        String url = ticketDocumentService.uploadPdf("booking-1", "ET-001", bytes);

        assertThat(url).isEqualTo("https://minio.example/sandbox/tickets/booking-1/ET-001.pdf");
    }
}
