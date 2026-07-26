package com.guentours.reseller.web;

import com.guentours.booking.domain.Booking;
import com.guentours.booking.domain.BookingStatus;
import com.guentours.booking.domain.OfferType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * DTO de réponse représentant une vente/réservation attribuée à un revendeur.
 */
public record ResellerBookingResponse(
        String id,
        String resellerId,
        String contactEmail,
        OfferType offerType,
        String summary,
        LocalDateTime ticketingDeadline,
        String pnrCode,
        BigDecimal totalAmount,
        String currency,
        BookingStatus status,
        int travelerCount,
        Instant createdAt
) {

    /**
     * Factory method pour mapper une entité Booking vers le DTO de réponse.
     */
    public static ResellerBookingResponse from(Booking booking) {
        if (booking == null) {
            return null;
        }

        // Extraction du montant et de la devise depuis le composant Money
        BigDecimal amount = booking.getPrice() != null ? booking.getPrice().amount() : BigDecimal.ZERO;
        String currency = booking.getPrice() != null ? booking.getPrice().currency() : "XAF";

        // Détermination du résumé fonctionnel (Origine ➔ Destination ou Nom de l'hôtel)
        String summary = buildSummary(booking);

        // Nombre total de voyageurs
        int travelerCount = booking.getTravelers() != null ? booking.getTravelers().size() : 0;

        return new ResellerBookingResponse(
                booking.getId(),
                booking.getResellerId(),
                booking.getContactEmail(),
                booking.getOfferType(),
                summary,
                booking.getTicketingDeadline(),
                booking.getProviderConfirmationNumber(),
                amount,
                currency,
                booking.getStatus(),
                travelerCount,
                booking.getCreatedAt()
        );
    }

    /**
     * Génère un libellé synthétique en fonction du type d'offre (Vol ou Hôtel).
     */
    private static String buildSummary(Booking booking) {
        if (booking.getOfferType() == OfferType.FLIGHT) {
            if (booking.getOrigin() != null && booking.getDestination() != null) {
                return booking.getOrigin() + " ➔ " + booking.getDestination();
            }
            return booking.getAirline() != null ? "Vol " + booking.getAirline() : "Réservation Vol";
        } else if (booking.getOfferType() == OfferType.HOTEL) {
            return booking.getHotelName() != null ? booking.getHotelName() : "Réservation Hôtel";
        }
        return "Réservation";
    }
}