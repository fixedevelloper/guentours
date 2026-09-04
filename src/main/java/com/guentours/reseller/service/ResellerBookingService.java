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
import org.springframework.transaction.annotation.Propagation;
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
     * {@code NOT_SUPPORTED}, overriding the class-level read-only default: {@code checkout()}
     * relies on its own {@code createPendingBooking} sub-call committing independently *before*
     * it fires the provider hold off-thread ({@code completeHold}, @Async) - wrapping this method
     * in a transaction of its own would instead fold that insert into this one, leaving it
     * uncommitted (and invisible to completeHold's own connection) for as long as this method
     * keeps running, racing (and sometimes losing) an async step that assumes it's already durable.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ResellerBookingResponse createBookingHold(ResellerCheckoutRequest req, Authentication authentication) {
        Reseller reseller = resolveConnectedReseller(authentication);
        Booking booking = bookingService.checkout(req.checkoutRequest());
        bookingRepository.assignReseller(booking.getId(), reseller.getId());
        booking.assignReseller(reseller.getId());
        return ResellerBookingResponse.from(booking);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ResellerBookingResponse createBookingMultiCityHold(MultiCityCheckoutRequest req, Authentication authentication) {
        Reseller reseller = resolveConnectedReseller(authentication);
        Booking booking = bookingService.checkoutMultiCity(req);
        bookingRepository.assignReseller(booking.getId(), reseller.getId());
        booking.assignReseller(reseller.getId());
        return ResellerBookingResponse.from(booking);
    }

    /** Resolves the reseller behind the currently authenticated principal, or 403s if the account isn't a reseller. */
    private Reseller resolveConnectedReseller(Authentication authentication) {
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        log.warn(principal.getRole());
        String resellerId = principal.getResellerId();
        if (resellerId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce compte n'est pas un compte revendeur");
        }
        return resellerService.findById(resellerId);
    }
}