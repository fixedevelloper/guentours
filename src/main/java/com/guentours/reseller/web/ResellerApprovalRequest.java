package com.guentours.reseller.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ResellerApprovalRequest(
        @NotNull(message = "Le taux de commission est obligatoire")
        @DecimalMin(value = "0.0", message = "La commission ne peut pas être négative")
        @DecimalMax(value = "1.0", message = "La commission ne peut pas dépasser 1.0 (100%)")
        @Digits(integer = 1, fraction = 4, message = "Format de taux invalide (maximum 4 décimales, ex: 0.1500)")
        BigDecimal commissionRate
) {}