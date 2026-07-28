package com.guentours.geo;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Lets an admin trigger an immediate reload instead of waiting for the 30-day schedule, and manage
 *  hotel_cities rows directly (see {@code /api/admin/**} in SecurityConfig for the role gate). */
@RestController
public class GeoAdminController {

    private final ReferenceDataSyncService syncService;
    private final HotelCityAdminService cityAdminService;

    public GeoAdminController(ReferenceDataSyncService syncService, HotelCityAdminService cityAdminService) {
        this.syncService = syncService;
        this.cityAdminService = cityAdminService;
    }

    @PostMapping("/api/admin/geo/airports/sync")
    public Map<String, Integer> syncAirports() {
        return Map.of("synced", syncService.syncAirports());
    }

    @PostMapping("/api/admin/geo/cities/sync")
    public Map<String, Integer> syncCities() {
        return Map.of("synced", syncService.syncHotelCities());
    }

    @GetMapping("/api/admin/geo/cities")
    public Page<HotelCityAdminResponse> listCities(@RequestParam(value = "q", required = false) String query,
                                                    Pageable pageable) {
        return cityAdminService.list(query, pageable).map(HotelCityAdminResponse::from);
    }

    @PostMapping("/api/admin/geo/cities")
    @ResponseStatus(HttpStatus.CREATED)
    public HotelCityAdminResponse createCity(@Valid @RequestBody HotelCityUpsertRequest req) {
        return HotelCityAdminResponse.from(cityAdminService.create(req));
    }

    @PutMapping("/api/admin/geo/cities/{id}")
    public HotelCityAdminResponse updateCity(@PathVariable Long id, @Valid @RequestBody HotelCityUpsertRequest req) {
        return HotelCityAdminResponse.from(cityAdminService.update(id, req));
    }

    @DeleteMapping("/api/admin/geo/cities/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCity(@PathVariable Long id) {
        cityAdminService.delete(id);
    }
}
