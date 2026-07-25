package com.guentours.reseller.web;

import com.guentours.reseller.domain.Reseller;
import com.guentours.reseller.domain.ResellerStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record ResellerResponse(
        String id,
        String companyName,
        String contactName,
        String email,
        String promoCode,
        BigDecimal commissionRate,
        ResellerStatus status,
        Instant createdAt
) {
    public static ResellerResponse from(Reseller r) {
        return new ResellerResponse(r.getId(), r.getCompanyName(), r.getContactName(), r.getEmail(),
                r.getPromoCode(), r.getCommissionRate(), r.getStatus(), r.getCreatedAt());
    }
}