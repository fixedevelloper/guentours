package com.guentours.destination.web;

import com.guentours.destination.FeaturedDestinationAdminResponse;
import com.guentours.destination.FeaturedDestinationService;
import com.guentours.destination.FeaturedDestinationUpsertRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Admin CRUD over featured destinations, plus a manual trigger for the booking-volume
 *  auto-suggestion (see {@code /api/admin/**} in SecurityConfig for the role gate). */
@RestController
@RequestMapping("/api/admin/destinations")
public class DestinationAdminController {

    private final FeaturedDestinationService service;
    private final int refreshTopN;

    public DestinationAdminController(FeaturedDestinationService service,
                                      @Value("${app.destinations.refresh-top-n:10}") int refreshTopN) {
        this.service = service;
        this.refreshTopN = refreshTopN;
    }

    @GetMapping
    public List<FeaturedDestinationAdminResponse> list() {
        return service.getAll().stream().map(FeaturedDestinationAdminResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeaturedDestinationAdminResponse create(@Valid @RequestBody FeaturedDestinationUpsertRequest req) {
        return FeaturedDestinationAdminResponse.from(service.create(req));
    }

    @PutMapping("/{id}")
    public FeaturedDestinationAdminResponse update(@PathVariable String id,
                                                   @Valid @RequestBody FeaturedDestinationUpsertRequest req) {
        return FeaturedDestinationAdminResponse.from(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    /** Suggests new destinations from real flight booking volume without touching existing rows. */
    @PostMapping("/refresh-from-bookings")
    public Map<String, Integer> refreshFromBookings() {
        return Map.of("added", service.refreshFromBookings(refreshTopN));
    }
}
