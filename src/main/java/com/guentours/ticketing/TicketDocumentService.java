package com.guentours.ticketing;

import com.guentours.booking.domain.Booking;
import com.guentours.reseller.domain.Reseller;
import com.guentours.reseller.domain.ResellerRepository;
import com.guentours.storage.StorageService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

/**
 * Renders a branded PDF version of an e-ticket (Guen Tours' own logo, plus the reseller's when
 * the booking was sold through one - {@link Booking#getResellerId()}) and uploads it to MinIO.
 * Kept package-private: only {@link ETicketService} calls this, same visibility convention as
 * {@code EmailService} elsewhere in the codebase.
 */
@Service
class TicketDocumentService {

    private static final Logger log = LoggerFactory.getLogger(TicketDocumentService.class);
    private static final DateTimeFormatter ISSUED_AT_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH).withZone(ZoneId.systemDefault());

    private final TemplateEngine templateEngine;
    private final ResellerRepository resellerRepository;
    private final StorageService storageService;
    private final String logoBase64;

    TicketDocumentService(TemplateEngine templateEngine, ResellerRepository resellerRepository,
                          StorageService storageService) {
        this.templateEngine = templateEngine;
        this.resellerRepository = resellerRepository;
        this.storageService = storageService;
        this.logoBase64 = loadLogoBase64();
    }

    /**
     * Read once at startup rather than per-render: the logo is a bundled classpath resource
     * (copied from the frontend's own public/logo.png), not something that changes at runtime.
     */
    private String loadLogoBase64() {
        try (var in = new ClassPathResource("branding/guentours-logo.png").getInputStream()) {
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(in.readAllBytes());
        } catch (IOException e) {
            log.warn("Could not load Guen Tours logo for ticket PDFs: {}", e.getMessage());
            return null;
        }
    }

    byte[] renderPdf(Booking booking, String ticketNumber) {
        Context context = new Context();
        context.setVariable("logoBase64", logoBase64);
        context.setVariable("ticketNumber", ticketNumber);
        context.setVariable("confirmationCode", booking.getProviderConfirmationNumber());
        context.setVariable("bookingReference", booking.getId());
        context.setVariable("passengerContact", booking.getContactEmail());
        context.setVariable("offerType", booking.getOfferType().name());
        context.setVariable("issuedAt", ISSUED_AT_FORMAT.format(Instant.now()));

        Reseller reseller = booking.getResellerId() != null
                ? resellerRepository.findById(booking.getResellerId()).orElse(null)
                : null;
        context.setVariable("resellerLogoUrl", reseller != null ? reseller.getLogoUrl() : null);
        context.setVariable("resellerCompanyName", reseller != null ? reseller.getCompanyName() : null);

        String html = templateEngine.process("tickets/ticket", context);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render ticket PDF for " + ticketNumber, e);
        }
    }

    String uploadPdf(String bookingId, String ticketNumber, byte[] pdfBytes) {
        return storageService.upload("tickets/" + bookingId, pdfBytes, "application/pdf", ticketNumber + ".pdf");
    }
}
