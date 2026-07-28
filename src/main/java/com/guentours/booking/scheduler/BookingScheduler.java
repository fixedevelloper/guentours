package com.guentours.booking.scheduler;

import com.guentours.booking.ExpiredBookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingScheduler {

    private final ExpiredBookingService expiredBookingService;

    /**
     * S'exécute 5 secondes après le démarrage, puis toutes les 5 minutes.
     */
    @Scheduled(
            fixedDelayString = "${app.booking.expiry-check-interval-ms:300000}",
            initialDelayString = "${app.booking.expiry-check-initial-delay-ms:60000}" // 5 secondes au lieu de 60s
    )
    public void scheduleExpiredPayLaterCleanup() {
        log.info("Lancement du scheduler de nettoyage des réservations PAY_LATER expirées...");
        expiredBookingService.cancelExpiredPayLaterBookings();
    }
}