package com.guentours.booking;

import com.guentours.booking.domain.Booking;
import com.guentours.booking.domain.BookingRepository;
import com.guentours.booking.domain.BookingStatus;
import com.guentours.booking.domain.PaymentPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpiredBookingService {

    private final BookingRepository bookingRepository;
    // Injecter ici un service d'intégration avec le Provider (ex: ProviderService) si besoin d'annuler le PNR chez le fournisseur

    /**
     * Traite l'annulation des réservations PAY_LATER expirées (créées il y a plus de 2h).
     */
    public void cancelExpiredPayLaterBookings() {
        // Seuil : Il y a exactement 2 heures
        Instant twoHoursAgo = Instant.now().minus(2, ChronoUnit.HOURS);

        // On cible les statuts non-payés/non-confirmés
        List<BookingStatus> pendingStatuses = List.of(
                BookingStatus.PENDING_PAYMENT,
                BookingStatus.DEPOSIT_PAID
        );

        List<Booking> expiredBookings = bookingRepository
                .findByPaymentPlanAndStatusInAndCreatedAtBefore(
                        PaymentPlan.PAY_LATER,
                        pendingStatuses,
                        twoHoursAgo
                );

        if (expiredBookings.isEmpty()) {
            return;
        }

        log.info("Trouvé {} réservation(s) PAY_LATER expirée(s) à annuler.", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            cancelSingleBooking(booking);
        }
    }

    /**
     * Annule une réservation de manière isolée pour éviter qu'une erreur sur un enregistrement ne bloque les autres.
     */
    @Transactional
    public void cancelSingleBooking(Booking booking) {
        try {
            log.info("Annulation de la réservation expirée ID: {}", booking.getId());

            // 1. Si un PNR a été généré chez le provider, annuler la réservation chez le partenaire/fournisseur
            if (booking.getProviderConfirmationNumber() != null) {
                // providerService.cancelPnr(booking.getProviderType(), booking.pnrCodes());
            }

            // 2. Mettre à jour l'état dans le domaine
            booking.markCancelled();
            booking.markFailed("Réservation PAY_LATER expirée (délai de 2h dépassé sans paiement complet).");

            bookingRepository.save(booking);

            // 3. (Optionnel) Envoyer un email au client pour notifier l'annulation
            // notificationService.sendBookingCancelledEmail(booking);

        } catch (Exception e) {
            log.error("Échec lors de l'annulation automatique de la réservation ID: {}", booking.getId(), e);
        }
    }
}
