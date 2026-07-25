package com.guentours.reseller.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ResellerWithdrawalRequest(
        @NotNull(message = "Le montant est obligatoire")
        @Positive(message = "Le montant doit être supérieur à zéro")
        BigDecimal amount,

        String currency,

        @NotBlank(message = "Le mode de paiement est obligatoire")
        String paymentMethod,

        @NotBlank(message = "Les détails du paiement sont obligatoires")
        String paymentDetails
) {}