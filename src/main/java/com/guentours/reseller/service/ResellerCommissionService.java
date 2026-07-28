package com.guentours.reseller.service;

import com.guentours.booking.domain.Booking;
import com.guentours.booking.domain.BookingRepository;
import com.guentours.reseller.domain.Reseller;
import com.guentours.reseller.domain.ResellerCommissionEntry;
import com.guentours.reseller.domain.ResellerCommissionRepository;
import com.guentours.reseller.domain.ResellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResellerCommissionService {

    private final BookingRepository bookingRepository;
    private final ResellerRepository resellerRepository;
    private final ResellerCommissionRepository commissionEntryRepository;

    /**
     * Point d'entrée à appeler dès qu'une réservation passe à un statut payé/confirmé.
     * Idempotent : si une commission existe déjà pour ce booking, on ne rejoue rien.
     */
    @Transactional
    public void handleBookingPaymentConfirmed(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Réservation introuvable : " + bookingId));

        String resellerId = booking.getResellerId();
        if (resellerId == null) {
            log.debug("Booking {} sans revendeur associé, aucune commission à générer.", bookingId);
            return;
        }

        if (commissionEntryRepository.existsByBookingId(bookingId)) {
            log.debug("Commission déjà générée pour le booking {}, on ignore (idempotence).", bookingId);
            return;
        }

        Reseller reseller = resellerRepository.findById(resellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Revendeur introuvable : " + resellerId));

        BigDecimal bookingAmount = booking.getPrice() != null ? booking.getPrice().amount() : BigDecimal.ZERO;
        String currency = booking.getPrice() != null ? booking.getPrice().currency() : "XAF";

        ResellerCommissionEntry entry = new ResellerCommissionEntry(
                reseller.getId(),
                bookingId,
                bookingAmount,
                reseller.getCommissionRate(),
                currency
        );

        // Le paiement est confirmé : la commission devient immédiatement disponible pour retrait.
        entry.approve();
        commissionEntryRepository.save(entry);

        reseller.creditWallet(entry.getAmount());
        resellerRepository.save(reseller);

        log.info("Commission de {} {} créditée au revendeur {} pour le booking {}",
                entry.getAmount(), entry.getCurrency(), reseller.getId(), bookingId);
    }

    /**
     * À appeler si une réservation déjà commissionnée est annulée/remboursée :
     * on annule la commission et on débite le wallet si elle avait déjà été créditée.
     */
    @Transactional
    public void handleBookingCancelledOrRefunded(String bookingId) {
        commissionEntryRepository.findByBookingId(bookingId).ifPresent(entry -> {
            if (entry.getStatus().name().equals("PAID")) {
                log.warn("Commission {} déjà versée, impossible de l'annuler automatiquement.", entry.getId());
                return;
            }
            boolean wasAvailable = entry.getStatus().name().equals("AVAILABLE");
            entry.cancel();
            commissionEntryRepository.save(entry);

            if (wasAvailable) {
                Reseller reseller = resellerRepository.findById(entry.getResellerId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Revendeur introuvable : " + entry.getResellerId()));
                reseller.debitWallet(entry.getAmount());
                resellerRepository.save(reseller);
                log.info("Commission annulée, wallet du revendeur {} débité de {}", reseller.getId(), entry.getAmount());
            }
        });
    }
}