package com.guentours.search.web;

import com.guentours.search.domain.Seat;

import java.util.List;

public record SeatMapResponse(int rows, List<String> columns, List<Seat> seats) {
}
