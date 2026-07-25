package com.guentours.reseller.service;

import com.guentours.booking.domain.Booking;
import com.guentours.booking.domain.BookingRepository;
import com.guentours.reseller.domain.ResellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResellerBookingService {

    private final BookingRepository bookingRepository;
    private final ResellerRepository resellerRepository;

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
}