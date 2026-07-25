package com.guentours.reseller.web;

import jakarta.validation.constraints.NotBlank;

public record WithdrawalApprovalRequest(
        @NotBlank(message = "La référence de transaction est obligatoire")
        String transactionReference,
        String adminNotes
) {}