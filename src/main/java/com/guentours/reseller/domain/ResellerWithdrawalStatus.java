package com.guentours.reseller.domain;

/**
 * Cycle de vie d'une demande de retrait effectuée par un revendeur.
 */
public enum ResellerWithdrawalStatus {

    /** Demande soumise par le revendeur, en attente de validation. */
    PENDING,

    /** En cours de traitement par le service financier ou le système de paiement (MoMo/Orange/Virement). */
    PROCESSING,

    /** Demande validée et fonds transférés au revendeur. */
    APPROVED,

    /** Demande rejetée (ex: coordonnées invalides, motif de fraude). */
    REJECTED
}