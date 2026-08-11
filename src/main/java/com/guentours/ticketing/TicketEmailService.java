package com.guentours.ticketing;

import com.guentours.storage.StorageService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Sends a ticket's PDF by email, on demand (the "share" button - see {@link ETicketService#sendByEmail}).
 * Talks to {@code JavaMailSender}/{@code TemplateEngine} directly rather than reusing
 * {@code notification.EmailService} - both are generic Spring Boot autoconfigured beans, not owned
 * by the notification module, and {@code notification} itself needs to depend on {@code ticketing}
 * (to attach a ticket's PDF to the automatic booking-confirmed email, see
 * {@link ETicketService#getForBookingInternal}) - the reverse dependency here would close a cycle.
 */
@Service
class TicketEmailService {

    private static final Logger log = LoggerFactory.getLogger(TicketEmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final StorageService storageService;
    private final String fromAddress;

    TicketEmailService(JavaMailSender mailSender, TemplateEngine templateEngine, StorageService storageService,
                       @Value("${app.mail.from:no-reply@guentours.com}") String fromAddress) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.storageService = storageService;
        this.fromAddress = fromAddress;
    }

    /** Unlike the fire-and-forget automatic confirmation email, this is user-initiated (they
     *  clicked "share") - a failure is thrown rather than swallowed, so the caller gets a real
     *  error instead of silently believing the share succeeded. */
    void sendTicketByEmail(ETicket ticket, String recipientEmail) {
        byte[] pdfBytes = storageService.download(ticket.getPdfUrl());

        Context context = new Context();
        context.setVariable("ticketNumber", ticket.getTicketNumber());
        context.setVariable("bookingReference", ticket.getBookingId());
        String html = templateEngine.process("email/ticket-share", context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(recipientEmail);
            helper.setSubject("Votre billet électronique - " + ticket.getTicketNumber());
            helper.setText(html, true);
            helper.addAttachment(ticket.getTicketNumber() + ".pdf", new ByteArrayResource(pdfBytes), "application/pdf");
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send ticket {} by email to {}: {}", ticket.getTicketNumber(), recipientEmail, ex.getMessage());
            throw new IllegalStateException("Failed to send ticket by email", ex);
        }
    }
}
