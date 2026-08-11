package com.guentours.partners.event;

/**
 * {@code partnerType} is the {@code PartnerType} enum's {@link Enum#name()} rather than the enum
 * itself - the only consumer (user.event.PartnerApprovedEventListener) immediately turns it back
 * into a String for PartnerRoleMapper.fromPartnerType anyway, and keeping the enum out of this
 * event's signature avoids a user -> partners module dependency that closed a Modulith cycle with
 * partners -> security -> user.
 */
public record PartnerApprovedEvent(
        String partnerId,
        String email,
        String companyName,
        String contactName,
        String partnerType
) {}
