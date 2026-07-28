package com.guentours.reseller.service;

import com.guentours.booking.BookingService;
import com.guentours.booking.domain.Booking;
import com.guentours.booking.domain.BookingRepository;
import com.guentours.booking.web.MultiCityCheckoutRequest;
import com.guentours.reseller.domain.Reseller;
import com.guentours.reseller.domain.ResellerRepository;
import com.guentours.reseller.web.ResellerBookingResponse;
import com.guentours.reseller.web.ResellerCheckoutRequest;
import com.guentours.security.AppUserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ResellerBookingService {

    private final BookingRepository bookingRepository;
    private final ResellerRepository resellerRepository;
    private final ResellerService resellerService;
    private final BookingService bookingService;

    public ResellerBookingService(BookingRepository bookingRepository, ResellerRepository resellerRepository,
                                  ResellerService resellerService, BookingService bookingService) {
        this.bookingRepository = bookingRepository;
        this.resellerRepository = resellerRepository;
        this.resellerService = resellerService;
        this.bookingService = bookingService;
    }

    /**
     * Récupère la liste paginée des réservations réalisées avec le code promo ou via l'espace d'un revendeur.
     */
    public Page<Booking> findByResellerId(String resellerId, Pageable pageable) {
        log.debug("Récupération des réservations pour le revendeur ID: {}", resellerId);
        if (!resellerRepository.existsById(resellerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun revendeur trouvé avec l'identifiant : " + resellerId);
        }
        return bookingRepository.findByResellerId(resellerId, pageable);
    }

    /**
     * Delegates the actual hold creation to the platform's booking flow, then tags the
     * resulting booking with the connected reseller's id so commission crediting can happen
     * later at payment confirmation.
     */
    @Transactional
    public ResellerBookingResponse createBookingHold(ResellerCheckoutRequest req, Authentication authentication) {
        Reseller reseller = resolveConnectedReseller(authentication);
        Booking booking = bookingService.checkout(req.checkoutRequest());
        booking.assignReseller(reseller.getId());
        Booking saved = bookingRepository.save(booking);
        return ResellerBookingResponse.from(saved);
    }

    @Transactional
    public ResellerBookingResponse createBookingMultiCityHold(MultiCityCheckoutRequest req, Authentication authentication) {
        Reseller reseller = resolveConnectedReseller(authentication);
        Booking booking = bookingService.checkoutMultiCity(req);
        booking.assignReseller(reseller.getId());
        Booking saved = bookingRepository.save(booking);
        return ResellerBookingResponse.from(saved);
    }

    /** Resolves the reseller behind the currently authenticated principal, or 403s if the account isn't a reseller. */
    private Reseller resolveConnectedReseller(Authentication authentication) {
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        String resellerId = principal.getResellerId();
        if (resellerId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce compte n'est pas un compte revendeur");
        }
        return resellerService.findById(resellerId);
    }
}