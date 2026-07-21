package com.guentours.partner.web;


import com.guentours.partner.domain.Partner;
import com.guentours.partner.domain.PartnerStatus;
import com.guentours.partner.domain.PartnerType;

import java.time.Instant;

public record PartnerResponse(
        String id,
        PartnerType partnerType,
        String companyName,
        String email,
        PartnerStatus status,
        Instant createdAt
) {
    public static PartnerResponse from(Partner p) {
        return new PartnerResponse(
                p.getId(), p.getPartnerType(), p.getCompanyName(),
                p.getEmail(), p.getStatus(), p.getCreatedAt()
        );
    }
}
