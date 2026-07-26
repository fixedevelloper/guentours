package com.guentours.reseller.service;

import com.guentours.booking.BookingService;
import com.guentours.booking.domain.Booking;
import com.guentours.booking.domain.BookingRepository;
import com.guentours.booking.web.CheckoutRequest;
import com.guentours.booking.web.MultiCityCheckoutRequest;
import com.guentours.reseller.domain.Reseller;
import com.guentours.reseller.domain.ResellerRepository;
import com.guentours.reseller.web.ResellerBookingResponse;
import com.guentours.reseller.web.ResellerCheckoutRequest;
import com.guentours.user.domain.User;
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
    private final BookingService bookingService; // ton service réel — non vu dans ce fil

    public ResellerBookingService(BookingRepository bookingRepository, ResellerRepository resellerRepository, ResellerService resellerService, BookingService bookingService /*, BookingService bookingService */) {
        this.bookingRepository = bookingRepository;
        this.resellerRepository = resellerRepository;
        this.resellerService = resellerService;
        // this.bookingService = bookingService;
        this.bookingService = bookingService;
    }
    /**
     * Récupère la liste paginée des réservations réalisées avec le code promo ou via l'espace d'un revendeur.
     *
     * @param resellerId Identifiant du revendeur
     * @param pageable   Options de pagination et de tri
     * @return Page des réservations associées au revendeur
     */
    public Page<Booking> findByResellerId(String resellerId, Pageable pageable) {
        log.debug("Récupération des réservations pour le revendeur ID: {}", resellerId);

        // Validation de l'existence du revendeur
        if (!resellerRepository.existsById(resellerId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Aucun revendeur trouvé avec l'identifiant : " + resellerId
            );
        }

        return bookingRepository.findByResellerId(resellerId, pageable);
    }

    /**
     * Resolves the reseller from the promo code (if any and still APPROVED), delegates the
     * actual hold creation to the platform's booking flow, then tags the resulting booking
     * with resellerId so commission crediting can happen later at payment confirmation.
     */
    public ResellerBookingResponse createBookingHold(ResellerCheckoutRequest req, Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        Reseller reseller = resellerService.findById(user.getResellerId());

        Booking booking = bookingService.checkout(req.checkoutRequest());

        if (reseller != null) {
            booking.assignReseller(reseller.getId());
            bookingRepository.save(booking);
        }

        return ResellerBookingResponse.from(booking);
    }
    public ResellerBookingResponse createBookingMultiCityHold(MultiCityCheckoutRequest req, Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        Reseller reseller = resellerService.findById(user.getResellerId());

        Booking booking = bookingService.checkoutMultiCity(req);

        if (reseller != null) {
            booking.assignReseller(reseller.getId());
            bookingRepository.save(booking);
        }

        return ResellerBookingResponse.from(booking);
    }
}