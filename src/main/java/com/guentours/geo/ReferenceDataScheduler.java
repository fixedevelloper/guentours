package com.guentours.geo;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reloads the airports/hotel_cities tables on a recurring schedule (default every 30 days,
 * see {@code app.reference-data.*} in application.yml). An admin can also trigger a sync
 * on demand via {@link GeoAdminController}.
 */
@Component
class ReferenceDataScheduler {

    private final ReferenceDataSyncService syncService;

    ReferenceDataScheduler(ReferenceDataSyncService syncService) {
        this.syncService = syncService;
    }

    @Scheduled(fixedRateString = "${app.reference-data.sync-interval-ms:2592000000}",
            initialDelayString = "${app.reference-data.sync-initial-delay-ms:0}")
    void syncAirports() {
        syncService.syncAirports();
    }

    @Scheduled(fixedRateString = "${app.reference-data.sync-interval-ms:2592000000}",
            initialDelayString = "${app.reference-data.sync-initial-delay-ms:0}")
    void syncHotelCities() {
        syncService.syncHotelCities();
    }
}
