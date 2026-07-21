package com.guentours.partner.event;


import com.guentours.partner.domain.PartnerType;

public record PartnerApprovedEvent(
        String partnerId,
        String email,
        String companyName,
        String contactName,
        PartnerType partnerType
) {}
