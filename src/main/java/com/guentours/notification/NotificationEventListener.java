package com.guentours.notification;

import com.guentours.booking.domain.Booking;
import com.guentours.booking.event.BookingConfirmedEvent;
import com.guentours.booking.event.BookingFailedEvent;
import com.guentours.booking.BookingService;
import com.guentours.newsletter.event.NewsletterSubscribedEvent;
import com.guentours.security.service.PasswordResetService;
import com.guentours.user.domain.User;
import com.guentours.user.event.PasswordResetRequestedEvent;
import com.guentours.user.event.UserAutoProvisionedEvent;
import com.guentours.user.service.UserService;
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
    private final PasswordResetService passwordResetService;
    NotificationEventListener(EmailService emailService, UserService userService, BookingService bookingService, PasswordResetService passwordResetService) {
        this.emailService = emailService;
        this.userService = userService;
        this.bookingService = bookingService;
        this.passwordResetService = passwordResetService;
    }

    @ApplicationModuleListener
    void on(UserAutoProvisionedEvent event) {
        userService.consumeTemporaryPassword(event.userId()).ifPresentOrElse(temporaryPassword -> {
            User user = userService.getById(event.userId());
            String body = """
                    Bonjour %s,

                    Un compte Guens travel a ete cree automatiquement pour vous lors de votre reservation.

                    Email :        %s
                    Mot de passe : %s

                    Nous vous recommandons de changer ce mot de passe lors de votre prochaine connexion.

                    L'equipe Guens travel
                    """.formatted(user.getFullName(), user.getEmail(), temporaryPassword);
            emailService.send(user.getEmail(), "Votre compte Guens travel a ete cree", body);
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

                Merci de voyager avec Guens travel.
                """.formatted(user.getFullName(), booking.getId(), booking.getProviderConfirmationNumber(),
                String.join(", ", booking.getETicketNumbers()));
        emailService.send(booking.getContactEmail(), "Confirmation de votre reservation Guens travel", body);
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

                L'equipe Guens travel
                """.formatted(user.getFullName(), booking.getId(), booking.getFailureReason());
        emailService.send(booking.getContactEmail(), "Probleme avec votre reservation Guens travel", body);
    }
    @ApplicationModuleListener
   void on(PasswordResetRequestedEvent event) {
                passwordResetService.consumePendingResetLink(event.userId()).ifPresentOrElse(resetLink -> {
                        User user = userService.getById(event.userId());
                        String body = """
                   Bonjour %s,
                    Vous avez demande la reinitialisation de votre mot de passe Guens travel.
                    Cliquez sur le lien ci-dessous pour choisir un nouveau mot de passe (valable 30 minutes) :
                    %s
                    Si vous n'etes pas a l'origine de cette demande, vous pouvez ignorer cet email.
                    L'equipe Guens travel
                    """.formatted(user.getFullName(), resetLink);
                        emailService.send(user.getEmail(), "Reinitialisation de votre mot de passe Guens travel", body);
                    }, () -> log.warn("No pending reset link available for user {} - not sending reset email", event.userId()));
            }

    @ApplicationModuleListener
    void on(NewsletterSubscribedEvent event) {
        String body = """
                Bonjour,

                Merci de vous etre inscrit(e) a la newsletter Guens travel !

                Vous recevrez desormais nos meilleures offres et alertes tarifaires.

                L'equipe Guens travel
                """;
        emailService.send(event.email(), "Bienvenue dans la newsletter Guens travel", body);
    }

}
