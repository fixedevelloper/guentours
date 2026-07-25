package com.guentours.reseller.service;

import com.guentours.reseller.domain.*;
import com.guentours.shared.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResellerCommissionService {

    private final ResellerRepository resellerRepository;
    private final ResellerCommissionRepository commissionRepository;

    /**
     * Crédite la commission du revendeur de manière idempotente lors de la confirmation d'une réservation.
     * Conserve le taux de commission appliqué au moment de l'achat et crée l'entrée au statut PENDING.
     *
     * @param resellerId   L'identifiant du revendeur.
     * @param bookingId    L'identifiant de la réservation.
     * @param bookingPrice Le montant total payé pour la réservation (objet Money).
     */
    @Transactional
    public void creditCommission(String resellerId, String bookingId, BigDecimal bookingPrice, String currency) {
        if (commissionRepository.existsByBookingId(bookingId)) {
            return;
        }

        Reseller reseller = resellerRepository.findById(resellerId).orElse(null);
        if (reseller == null || reseller.getStatus() != ResellerStatus.APPROVED) {
            return;
        }

        // Le constructeur gère le calcul du montant et l'arrondi automatiquement
        ResellerCommissionEntry entry = new ResellerCommissionEntry(
                resellerId,
                bookingId,
                bookingPrice,
                reseller.getCommissionRate(),
                currency
        );

        commissionRepository.save(entry);
    }
    /**
     * Libère la commission vers le solde disponible (AVAILABLE) une fois le vol/séjour consommé.
     *
     * @param bookingId L'identifiant de la réservation.
     */
    @Transactional
    public void releaseCommission(String bookingId) {
        commissionRepository.findByBookingId(bookingId).ifPresentOrElse(
                entry -> {
                    entry.markPaid();
                    log.info("Commission pour la réservation ID: {} débloquée au statut AVAILABLE.", bookingId);
                },
                () -> log.warn("Aucune commission trouvée à débloquer pour la réservation ID: {}", bookingId)
        );
    }

    /**
     * Annule la commission en cas d'annulation ou de remboursement de la réservation.
     *
     * @param bookingId L'identifiant de la réservation.
     */
    @Transactional
    public void cancelCommission(String bookingId) {
        commissionRepository.findByBookingId(bookingId).ifPresentOrElse(
                entry -> {
                    entry.cancel();
                    log.info("Commission pour la réservation ID: {} marquée comme CANCELLED.", bookingId);
                },
                () -> log.warn("Aucune commission trouvée à annuler pour la réservation ID: {}", bookingId)
        );
    }
}