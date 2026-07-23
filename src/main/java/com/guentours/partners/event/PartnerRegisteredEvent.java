package com.guentours.partners.event;

import com.guentours.partners.domain.PartnerType;

public record PartnerRegisteredEvent(String partnerId, String companyName, PartnerType partnerType) {}
