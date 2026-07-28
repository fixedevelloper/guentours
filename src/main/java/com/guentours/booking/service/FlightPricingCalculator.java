package com.guentours.booking.service;

import com.guentours.booking.domain.BookedTraveler;
import com.guentours.booking.web.TravelerRequest;
import com.guentours.provider.PassengerType;
import com.guentours.shared.Money;

import java.math.BigDecimal;
import java.util.List;

/**
 * Calcule le prix total d'un vol à partir du prix unitaire de l'offre et de la liste
 * des voyageurs. Règle tarifaire : ADULT et CHILD payent le tarif plein de l'offre,
 * INFANT est gratuit (billet sur les genoux, pas de siège facturé).
 */
public final class FlightPricingCalculator {

    private FlightPricingCalculator() {}

    public static Money computeTotalPrice(Money unitPrice, List<TravelerRequest> travelers) {
        long payingTravelers = travelers.stream()
                .filter(t -> t.type() != PassengerType.INFANT)
                .count();

        BigDecimal totalAmount = unitPrice.amount().multiply(BigDecimal.valueOf(payingTravelers));
        return new Money(totalAmount, unitPrice.currency());
    }

    public static Money multiplyByPayingTravelers(Money price, List<BookedTraveler> travelers) {
        return null;
    }
}