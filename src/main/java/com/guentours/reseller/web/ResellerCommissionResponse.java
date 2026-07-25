package com.guentours.reseller.web;

import com.guentours.reseller.domain.ResellerCommissionEntry;
import com.guentours.reseller.domain.ResellerCommissionStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record ResellerCommissionResponse(
        String id,
        String bookingId,
        BigDecimal amount,
        String currency,
        ResellerCommissionStatus status,
        Instant createdAt
) {
    public static ResellerCommissionResponse from(ResellerCommissionEntry e) {
        return new ResellerCommissionResponse(e.getId(), e.getBookingId(), e.getAmount(),
                e.getCurrency(), e.getStatus(), e.getCreatedAt());
    }
}