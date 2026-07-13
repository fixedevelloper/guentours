package com.guentours.notification;

import com.guentours.booking.Booking;
import com.guentours.booking.BookingConfirmedEvent;
import com.guentours.booking.BookingFailedEvent;
import com.guentours.booking.BookingService;
import com.guentours.user.User;
import com.guentours.user.UserAutoProvisionedEvent;
import com.guentours.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final EmailService emailService;
    private final UserService userService;
    private final BookingService bookingService;

    NotificationEventListener(EmailService emailService, UserService userService, BookingService bookingService) {
        this.emailService = emailService;
        this.userService = userService;
        this.bookingService = bookingService;
    }

    @ApplicationModuleListener
    void on(UserAutoProvisionedEvent event) {
        userService.consumeTemporaryPassword(event.userId()).ifPresentOrElse(temporaryPassword -> {
            User user = userService.getById(event.userId());
            String body = """
                    Bonjour %s,

                    Un compte Guentours a ete cree automatiquement pour vous lors de votre reservation.

                    Email :        %s
                    Mot de passe : %s

                    Nous vous recommandons de changer ce mot de passe lors de votre prochaine connexion.

                    L'equipe Guentours
                    """.formatted(user.getFullName(), user.getEmail(), temporaryPassword);
            emailService.send(user.getEmail(), "Votre compte Guentours a ete cree", body);
        }, () -> log.warn("No temporary password available for user {} - not sending welcome email", event.userId()));
    }

    @ApplicationModuleListener
    void on(BookingConfirmedEvent event) {
        Booking booking = bookingService.getById(event.bookingId());
        User user = userService.getById(booking.getUserId());
        String body = """
                Bonjour %s,

                Votre reservation est confirmee !

                Reference booking :      %s
                Code de confirmation :   %s
                Billets electroniques :  %s

                Merci de voyager avec Guentours.
                """.formatted(user.getFullName(), booking.getId(), booking.getProviderConfirmationNumber(),
                String.join(", ", booking.getETicketNumbers()));
        emailService.send(booking.getContactEmail(), "Confirmation de votre reservation Guentours", body);
    }

    @ApplicationModuleListener
    void on(BookingFailedEvent event) {
        Booking booking = bookingService.getById(event.bookingId());
        User user = userService.getById(booking.getUserId());
        String body = """
                Bonjour %s,

                Nous n'avons malheureusement pas pu confirmer votre reservation (reference %s) auprès du fournisseur.
                Motif : %s

                Le montant paye sera remboursé et notre equipe reviendra vers vous rapidement.

                L'equipe Guentours
                """.formatted(user.getFullName(), booking.getId(), booking.getFailureReason());
        emailService.send(booking.getContactEmail(), "Probleme avec votre reservation Guentours", body);
    }
}
