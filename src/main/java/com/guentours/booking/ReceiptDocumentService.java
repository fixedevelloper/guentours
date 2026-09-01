package com.guentours.booking;

import com.guentours.booking.domain.Booking;
import com.guentours.booking.domain.OfferType;
import com.guentours.shared.Money;
import com.guentours.user.domain.User;
import com.guentours.user.service.UserService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

/**
 * Renders an on-demand PDF receipt for a booking (the "Reçu PDF" action on the admin booking
 * detail page). Unlike {@code ticketing.TicketDocumentService}'s e-ticket PDFs, this is never
 * persisted to storage - it's streamed straight back to the caller for each request (see
 * {@code AdminBookingController#receipt}), so there's nothing to reuse or invalidate.
 * Public (unlike the package-private {@code TicketDocumentService}) because its only caller,
 * {@code booking.web.AdminBookingController}, lives in a sub-package - same visibility need as
 * {@link BookingService} itself.
 */
@Service
public class ReceiptDocumentService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptDocumentService.class);
    private static final DateTimeFormatter ISSUED_AT_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH).withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH);

    private final TemplateEngine templateEngine;
    private final UserService userService;
    private final String logoBase64;

    public ReceiptDocumentService(TemplateEngine templateEngine, UserService userService) {
        this.templateEngine = templateEngine;
        this.userService = userService;
        this.logoBase64 = loadLogoBase64();
    }

    /** Read once at startup rather than per-render, same reasoning as TicketDocumentService. */
    private String loadLogoBase64() {
        try (var in = new ClassPathResource("branding/guentours-logo.png").getInputStream()) {
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(in.readAllBytes());
        } catch (IOException e) {
            log.warn("Could not load Guen Tours logo for receipt PDFs: {}", e.getMessage());
            return null;
        }
    }

    public byte[] renderPdf(Booking booking) {
        User user = userService.getById(booking.getUserId());

        Context context = new Context();
        context.setVariable("logoBase64", logoBase64);
        context.setVariable("bookingReference", booking.getId());
        context.setVariable("offerType", booking.getOfferType().name());
        context.setVariable("status", booking.getStatus().name());
        context.setVariable("customerName", user.getFullName());
        context.setVariable("contactEmail", booking.getContactEmail());
        context.setVariable("price", formatMoney(booking.getPrice()));
        context.setVariable("amountDue", formatMoney(booking.amountDue()));
        context.setVariable("paymentCaptured", booking.isPaymentCaptured());
        context.setVariable("providerConfirmationNumber", booking.getProviderConfirmationNumber());
        context.setVariable("issuedAt", ISSUED_AT_FORMAT.format(Instant.now()));

        context.setVariable("isFlight", booking.getOfferType() == OfferType.FLIGHT);
        context.setVariable("airline", booking.getAirline());
        context.setVariable("flightNumber", booking.getFlightNumber());
        context.setVariable("origin", booking.getOrigin());
        context.setVariable("destination", booking.getDestination());
        context.setVariable("departureTime", formatDateTime(booking.getDepartureTime()));

        context.setVariable("isHotel", booking.getOfferType() == OfferType.HOTEL);
        context.setVariable("hotelName", booking.getHotelName());
        context.setVariable("cityCode", booking.getCityCode());
        context.setVariable("checkIn", formatDate(booking.getCheckIn()));
        context.setVariable("checkOut", formatDate(booking.getCheckOut()));

        context.setVariable("isCarRental", booking.getOfferType() == OfferType.CAR_RENTAL);
        context.setVariable("vehicleBrand", booking.getVehicleBrand());
        context.setVariable("vehicleModel", booking.getVehicleModel());
        context.setVariable("pickupCity", booking.getPickupCity());
        context.setVariable("dropoffCity", booking.getDropoffCity());
        context.setVariable("rentalStart", formatDate(booking.getRentalStart()));
        context.setVariable("rentalEnd", formatDate(booking.getRentalEnd()));

        context.setVariable("isFurnishedRental", booking.getOfferType() == OfferType.FURNISHED_RENTAL);
        context.setVariable("propertyTitle", booking.getPropertyTitle());
        context.setVariable("country", booking.getCountry());

        String html = templateEngine.process("receipts/receipt", context);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render receipt PDF for booking " + booking.getId(), e);
        }
    }

    private static String formatMoney(Money money) {
        return money == null ? null : money.amount().toPlainString() + " " + money.currency();
    }

    private static String formatDate(LocalDate date) {
        return date == null ? null : DATE_FORMAT.format(date);
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : DATETIME_FORMAT.format(dateTime);
    }
}
