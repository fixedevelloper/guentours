package com.guentours.user.service;

import com.guentours.user.domain.Role;

public final class PartnerRoleMapper {
    private PartnerRoleMapper() {}

    public static Role fromPartnerType(String partnerType) {
        return switch (partnerType) {
            case "AIRLINE" -> Role.PARTNER_AIRLINE;
            case "HOTEL" -> Role.PARTNER_HOTEL;
            case "CAR_RENTAL" -> Role.PARTNER_CAR_RENTAL;
            case "FURNISHED_RENTAL" -> Role.PARTNER_FURNISHED_RENTAL;
            default -> throw new IllegalArgumentException("Type de partenaire inconnu: " + partnerType);
        };
    }
}
