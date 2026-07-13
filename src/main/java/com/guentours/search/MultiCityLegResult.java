package com.guentours.search;

import java.time.LocalDate;
import java.util.List;

/** Harmonized offers for a single leg of a MULTI_CITY search, keyed by its position in the itinerary. */
public record MultiCityLegResult(
        int legIndex,
        String origin,
        String destination,
        LocalDate departureDate,
        List<HarmonizedFlightOffer> offers
) {
}
