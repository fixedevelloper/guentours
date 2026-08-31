package com.guentours.provider;

/**
 * One passenger type's baggage allowance on a {@link FlightSegmentDetail}. {@code rule} is a
 * provider free-text allowance (e.g. {@code "23 Kgs"}, possibly {@code "1 piece"} elsewhere) -
 * kept as-is rather than parsed into a structured weight, since the format isn't guaranteed
 * consistent across providers/routes/fares.
 */
public record BaggageRule(String paxType, String rule, Integer quantity, String size) {
}
