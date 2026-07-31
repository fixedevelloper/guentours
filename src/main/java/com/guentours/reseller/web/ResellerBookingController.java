package com.guentours.reseller.web;

import com.guentours.booking.domain.Booking;
import com.guentours.booking.web.MultiCityCheckoutRequest;
import com.guentours.reseller.service.ResellerBookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/reseller/bookings")
@RequiredArgsConstructor
public class ResellerBookingController {

    private final ResellerBookingService resellerBookingService;

    /**
     * Liste paginée des réservations réalisées avec le code promo ou via l'espace
     * d'un revendeur donné.
     * GET /api/reseller/bookings/{resellerId}
     */
    @GetMapping("/{resellerId}")
    @PreAuthorize("#resellerId == authentication.principal.resellerId or hasRole('ADMIN')")
    public ResponseEntity<Page<ResellerBookingResponse>> findByResellerId(
            @PathVariable String resellerId,
            Pageable pageable) {
        log.debug("GET bookings for reseller {}", resellerId);
        Page<Booking> bookings = resellerBookingService.findByResellerId(resellerId, pageable);
        return ResponseEntity.ok(bookings.map(ResellerBookingResponse::from));
    }

    /**
     * Crée un hold de réservation "simple ville" pour le revendeur connecté.
     * POST /api/reseller/bookings
     */
    @PostMapping
    public ResponseEntity<ResellerBookingResponse> createBookingHold(
            @RequestBody ResellerCheckoutRequest request,
            Authentication authentication) {
        ResellerBookingResponse response = resellerBookingService.createBookingHold(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Crée un hold de réservation multi-villes pour le revendeur connecté.
     * POST /api/reseller/bookings/multi-city
     */
    @PostMapping("/multi-city")
    public ResponseEntity<ResellerBookingResponse> createBookingMultiCityHold(
            @RequestBody MultiCityCheckoutRequest request,
            Authentication authentication) {
        ResellerBookingResponse response = resellerBookingService.createBookingMultiCityHold(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}