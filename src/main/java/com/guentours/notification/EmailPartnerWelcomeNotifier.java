package com.guentours.notification;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailPartnerWelcomeNotifier implements PartnerWelcomeNotifier {

    private static final Logger log = LoggerFactory.getLogger(EmailPartnerWelcomeNotifier.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.partner-login-url}")
    private String loginUrl;

    public EmailPartnerWelcomeNotifier(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void sendPartnerWelcomeEmail(String email, String contactName, String companyName, String tempPassword) {
        try {
            Context context = new Context();
            context.setVariable("contactName", contactName);
            context.setVariable("companyName", companyName);
            context.setVariable("email", email);
            context.setVariable("tempPassword", tempPassword);
            context.setVariable("loginUrl", loginUrl);

            String html = templateEngine.process("email/partner-welcome", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(email);
            helper.setSubject("Bienvenue sur Creativ Trips — votre compte partenaire est actif");
            helper.setText(html, true);

            mailSender.send(message);
        } catch (Exception e) {
            // Ne pas faire échouer la transaction du listener pour une erreur mail —
            // on log et on laisse Spring Modulith rejouer l'event si besoin.
            log.error("Échec envoi email de bienvenue partenaire à {}", email, e);
            throw new RuntimeException("Échec envoi email de bienvenue", e);
        }
    }
}
