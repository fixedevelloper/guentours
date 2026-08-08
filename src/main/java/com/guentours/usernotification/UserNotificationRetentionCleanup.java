package com.guentours.usernotification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class UserNotificationRetentionCleanup {

    private final UserNotificationService notificationService;
    private final int retentionReadDays;

    public UserNotificationRetentionCleanup(UserNotificationService notificationService,
            @Value("${app.notifications.retention-read-days:30}") int retentionReadDays) {
        this.notificationService = notificationService;
        this.retentionReadDays = retentionReadDays;
    }

    @Scheduled(cron = "0 30 3 * * *") // tous les jours à 3h30 du matin
    public void cleanup() {
        notificationService.cleanupReadOlderThan(Instant.now().minus(retentionReadDays, ChronoUnit.DAYS));
    }
}
