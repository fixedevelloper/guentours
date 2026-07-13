package com.guentours.search;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final FlightSearchService flightSearchService;
    private final HotelSearchService hotelSearchService;

    public SearchController(FlightSearchService flightSearchService, HotelSearchService hotelSearchService) {
        this.flightSearchService = flightSearchService;
        this.hotelSearchService = hotelSearchService;
    }

    @GetMapping("/flights")
    public ResponseEntity<List<HarmonizedFlightOffer>> searchFlights(@Valid @ModelAttribute FlightSearchRequest request) {
        return ResponseEntity.ok(flightSearchService.search(request));
    }

    @PostMapping("/flights/multi-city")
    public ResponseEntity<List<MultiCityItinerary>> searchMultiCityFlights(
            @Valid @RequestBody MultiCityFlightSearchRequest request) {
        return ResponseEntity.ok(flightSearchService.searchMultiCity(request));
    }

    @GetMapping("/hotels")
    public ResponseEntity<List<HarmonizedHotelOffer>> searchHotels(@Valid @ModelAttribute HotelSearchRequest request) {
        return ResponseEntity.ok(hotelSearchService.search(request));
    }
}
