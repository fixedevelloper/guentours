package com.guentours.reseller.web;

import jakarta.validation.constraints.NotBlank;

public record WithdrawalRejectionRequest(
        @NotBlank(message = "Le motif du rejet est obligatoire")
        String reason
) {}