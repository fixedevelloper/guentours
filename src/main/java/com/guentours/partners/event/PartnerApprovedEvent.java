package com.guentours.partners.event;

import com.guentours.partners.domain.PartnerType;

public record PartnerApprovedEvent(
        String partnerId,
        String email,
        String companyName,
        String contactName,
        PartnerType partnerType
) {}
