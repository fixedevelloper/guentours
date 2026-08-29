package com.guentours.provider;

/** Kind of purchasable extra offered alongside a flight offer. INSURANCE is GuenTours' own line
 *  (never sent to any provider); BAGGAGE/MEAL/SEAT come from a provider's ancillary API. */
public enum AncillaryType {
    BAGGAGE, MEAL, SEAT, INSURANCE
}
