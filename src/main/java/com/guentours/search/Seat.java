package com.guentours.search;

/** One seat in a flight's seat map, e.g. "12A". */
public record Seat(String seatNumber, boolean available) {
}
