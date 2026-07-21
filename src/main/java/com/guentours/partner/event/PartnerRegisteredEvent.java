package com.guentours.partner.event;

import com.guentours.partner.domain.PartnerType;

public record PartnerRegisteredEvent(String partnerId, String companyName, PartnerType partnerType) {}
