package com.guentours.provider;

/** An ancillary the guest picked at checkout, carrying back the opaque token needed to actually
 *  apply it with the provider at hold/book time (see {@link AncillaryOption#providerToken}). */
public record SelectedAncillary(AncillaryType type, String providerToken) {
}
