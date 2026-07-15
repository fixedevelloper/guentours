package com.guentours.search;

import java.util.List;

public record SeatMapResponse(int rows, List<String> columns, List<Seat> seats) {
}
