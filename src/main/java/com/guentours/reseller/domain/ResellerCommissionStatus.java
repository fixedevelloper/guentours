package com.guentours.reseller.domain;

/**
 * Représente le cycle de vie d'une commission attribuée à un revendeur.
 */
public enum ResellerCommissionStatus {

    /**
     * La réservation a été effectuée avec le code promo,
     * mais le paiement ou la confirmation finale est en attente (ex: PAY_LATER).
     */
    PENDING,

    /**
     * La réservation est confirmée et payée.
     * La commission est validée et ajoutée au solde disponible du revendeur.
     */
    AVAILABLE,

    /**
     * La commission a fait l'objet d'un retrait effectif par le revendeur.
     */
    PAID,

    /**
     * La réservation a été annulée ou remboursée. La commission est invalidée.
     */
    CANCELLED
}