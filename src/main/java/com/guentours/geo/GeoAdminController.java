package com.guentours.geo;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Lets an admin trigger an immediate reload instead of waiting for the 30-day schedule. */
@RestController
public class GeoAdminController {

    private final ReferenceDataSyncService syncService;

    public GeoAdminController(ReferenceDataSyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/api/admin/geo/airports/sync")
    public Map<String, Integer> syncAirports() {
        return Map.of("synced", syncService.syncAirports());
    }

    @PostMapping("/api/admin/geo/cities/sync")
    public Map<String, Integer> syncCities() {
        return Map.of("synced", syncService.syncHotelCities());
    }
}
