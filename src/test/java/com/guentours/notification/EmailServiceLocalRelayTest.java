package com.guentours.notification;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the real MailConfig-produced JavaMailSender against an embedded SMTP relay
 * (matching the host/port the "test" profile points at) to prove EmailService actually
 * delivers mail over the wire, not just that it builds a message object.
 */
@SpringBootTest
@ActiveProfiles("test")
class EmailServiceLocalRelayTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetup.SMTP.port(3025));

    @Autowired
    private EmailService emailService;

    @Test
    void deliversAPlainTextEmailToTheLocalRelay() throws Exception {
        emailService.send("traveler@example.com", "Test subject", "Test body");

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertThat(received).hasSize(1);
        assertThat(received[0].getAllRecipients()[0].toString()).isEqualTo("traveler@example.com");
        assertThat(received[0].getSubject()).isEqualTo("Test subject");
        assertThat(GreenMailUtil.getBody(received[0])).contains("Test body");
    }
}
