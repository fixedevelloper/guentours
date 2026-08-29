package com.guentours.provider;

import java.util.List;

/**
 * Cabin-grid placement/attributes for a {@code SEAT}-type {@link AncillaryOption}, letting the
 * frontend render an actual airplane seat map instead of a flat price list. Null on every other
 * {@link AncillaryType}. {@code totalRows}/{@code totalColumns}/{@code seatGroups}/{@code
 * cabinClass} describe the whole segment's grid and are repeated identically on every seat in
 * that segment (denormalized - simplest way to keep {@link AncillaryOption} a flat, uniform list).
 */
public record SeatLayout(
        String row,
        String column,
        String status,
        boolean exitRow,
        boolean accessible,
        boolean bassinet,
        boolean toilet,
        boolean galley,
        int totalRows,
        int totalColumns,
        List<String> seatGroups,
        String cabinClass
) {
}
