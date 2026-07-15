package com.guentours.search;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final FlightSearchService flightSearchService;
    private final HotelSearchService hotelSearchService;
    private final SeatMapService seatMapService;

    public SearchController(FlightSearchService flightSearchService, HotelSearchService hotelSearchService,
                             SeatMapService seatMapService) {
        this.flightSearchService = flightSearchService;
        this.hotelSearchService = hotelSearchService;
        this.seatMapService = seatMapService;
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

    /** Seat map for the seat-selection step at checkout, resolved from the same cached offer as checkout itself. */
    @GetMapping("/flights/seats")
    public ResponseEntity<SeatMapResponse> flightSeatMap(@RequestParam String offerId) {
        return ResponseEntity.ok(seatMapService.seatMapFor(offerId));
    }

    @GetMapping("/hotels")
    public ResponseEntity<List<HarmonizedHotelOffer>> searchHotels(@Valid @ModelAttribute HotelSearchRequest request) {
        return ResponseEntity.ok(hotelSearchService.search(request));
    }
}
