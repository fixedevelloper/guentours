package com.guentours.search.domain;

import com.guentours.shared.Money;

import java.util.Comparator;

/**
 * Best-effort "cheapest first" ordering for prices collected across providers during search
 * harmonization. Real providers do not always honor the requested currency (Travelport has
 * returned CNY for a search that requested USD), and {@link Money#compareTo} deliberately throws
 * on a currency mismatch - correct for monetary arithmetic, but that would crash the whole search
 * result ranking outright instead of just producing an imperfect order. This orders by currency
 * first (so same-currency prices still rank correctly relative to each other) then by amount,
 * rather than throwing.
 */
public final class PriceOrdering {

    public static final Comparator<Money> CHEAPEST_FIRST =
            Comparator.comparing(Money::currency).thenComparing(Money::amount);

    private PriceOrdering() {
    }

    public static boolean isCheaper(Money candidate, Money current) {
        return CHEAPEST_FIRST.compare(candidate, current) < 0;
    }
}
