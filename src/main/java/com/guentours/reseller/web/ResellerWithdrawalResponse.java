package com.guentours.reseller.web;

import com.guentours.reseller.domain.ResellerWithdrawal;
import com.guentours.reseller.domain.ResellerWithdrawalStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record ResellerWithdrawalResponse(
        String id,
        String resellerId,
        BigDecimal amount,
        BigDecimal remainingWallet,
        String currency,
        String paymentMethod,
        String paymentDetails,
        ResellerWithdrawalStatus status,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt,
        Instant processedAt
) {
    public static ResellerWithdrawalResponse from(ResellerWithdrawal entity) {
        if (entity == null) {
            return null;
        }

        return new ResellerWithdrawalResponse(
                entity.getId(),
                entity.getResellerId(),
                entity.getAmount(),
                entity.getRemainingWallet(),
                entity.getCurrency(),
                entity.getPaymentMethod(),
                entity.getPaymentDetails(),
                entity.getStatus(),
                entity.getRejectionReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getProcessedAt()
        );
    }
}