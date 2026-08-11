package com.guentours.notification;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    EmailService(JavaMailSender mailSender, @Value("${app.mail.from:no-reply@guentours.com}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            // A misconfigured/unreachable SMTP relay must never roll back the booking/payment
            // transaction that triggered this email - log and move on.
            log.error("Failed to send email to {}: {}", to, ex.getMessage());
        }
    }

    /**
     * Same delivery guarantees as {@link #send(String, String, String)} (never throws - a failed
     * send is logged and swallowed), but as a multipart message carrying one attachment (e.g. a
     * branded PDF ticket), same {@code MimeMessageHelper(message, true, "UTF-8")} pattern already
     * used by {@code EmailPartnerWelcomeNotifier}.
     */
    void sendWithAttachment(String to, String subject, String body, boolean html, byte[] attachmentBytes,
                            String attachmentFilename, String attachmentContentType) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, html);
            helper.addAttachment(attachmentFilename,
                    new org.springframework.core.io.ByteArrayResource(attachmentBytes), attachmentContentType);
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send email with attachment to {}: {}", to, ex.getMessage());
        }
    }
}
