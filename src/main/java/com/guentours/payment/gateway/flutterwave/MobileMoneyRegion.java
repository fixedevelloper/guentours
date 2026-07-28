package com.guentours.payment.gateway.flutterwave;

import java.util.Map;
import java.util.Set;

/**
 * Route un ISO2 vers la "région" mobile money supportée par le SDK Flutterwave.
 * Un pays absent de cette table n'a pas de mobile money exploitable via ce SDK.
 */
enum MobileMoneyRegion {
    FRANCOPHONE,  // CM, SN, CI, ML — pas de champ "network" requis
    GHANA,        // network requis
    UGANDA,       // network requis
    RWANDA,       // pas de champ "network" requis
    ZAMBIA,       // network requis
    MPESA;        // KE — pas de champ "network" requis

    private static final Map<String, MobileMoneyRegion> BY_ISO2 = Map.ofEntries(
            Map.entry("CM", FRANCOPHONE),
            Map.entry("SN", FRANCOPHONE),
            Map.entry("CI", FRANCOPHONE),
            Map.entry("ML", FRANCOPHONE),
            Map.entry("GH", GHANA),
            Map.entry("UG", UGANDA),
            Map.entry("RW", RWANDA),
            Map.entry("ZM", ZAMBIA),
            Map.entry("KE", MPESA)
    );

    static final Set<MobileMoneyRegion> REQUIRES_NETWORK = Set.of(GHANA, UGANDA, ZAMBIA);

    static MobileMoneyRegion resolve(String iso2) {
        MobileMoneyRegion region = BY_ISO2.get(iso2 == null ? null : iso2.toUpperCase());
        if (region == null) {
            throw new UnsupportedMobileMoneyCountryException(iso2);
        }
        return region;
    }
}
