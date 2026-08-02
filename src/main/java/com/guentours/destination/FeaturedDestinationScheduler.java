package com.guentours.destination;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps the homepage's featured destinations fresh with real booking data on a recurring
 * schedule (default daily, see {@code app.destinations.*} in application.yml). An admin can also
 * trigger a refresh on demand via {@code POST /api/admin/destinations/refresh-from-bookings}.
 */
@Component
class FeaturedDestinationScheduler {

    private final FeaturedDestinationService service;
    private final int topN;

    FeaturedDestinationScheduler(FeaturedDestinationService service,
                                 @Value("${app.destinations.refresh-top-n:10}") int topN) {
        this.service = service;
        this.topN = topN;
    }

    @Scheduled(fixedRateString = "${app.destinations.refresh-interval-ms:86400000}",
            initialDelayString = "${app.destinations.refresh-initial-delay-ms:60000}")
    void refresh() {
        service.refreshFromBookings(topN);
    }
}
