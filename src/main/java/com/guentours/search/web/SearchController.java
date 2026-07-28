package com.guentours.search.web;

import com.guentours.provider.HotelDetail;
import com.guentours.provider.RoomOffer;
import com.guentours.provider.VehicleSearchCriteria;
import com.guentours.search.domain.*;
import com.guentours.search.service.FlightSearchService;
import com.guentours.search.service.HotelSearchService;
import com.guentours.search.service.PropertySearchService;
import com.guentours.search.service.SeatMapService;
import com.guentours.search.service.VehicleSearchService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final FlightSearchService flightSearchService;
    private final HotelSearchService hotelSearchService;
    private final SeatMapService seatMapService;
    private final VehicleSearchService vehicleSearchService;
    private final PropertySearchService propertySearchService;

    public SearchController(FlightSearchService flightSearchService, HotelSearchService hotelSearchService,
                            SeatMapService seatMapService, VehicleSearchService vehicleSearchService,
                            PropertySearchService propertySearchService) {
        this.flightSearchService = flightSearchService;
        this.hotelSearchService = hotelSearchService;
        this.seatMapService = seatMapService;
        this.vehicleSearchService = vehicleSearchService;
        this.propertySearchService = propertySearchService;
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

    /** Récupère la fiche détaillée d'un hôtel à partir de l'identifiant d'offre mis en cache. */
    @GetMapping("/hotels/details")
    public ResponseEntity<HotelDetail> getHotelDetail(@RequestParam String offerId) {
        return ResponseEntity.ok(hotelSearchService.getDetailHotel(offerId));
    }

    @GetMapping("/hotels/get-rooms")
    public ResponseEntity<List<RoomOffer>> getRoomOffers(@RequestParam String offerId) {
        return ResponseEntity.ok(hotelSearchService.getRoomHotels(offerId));
    }

    @GetMapping("/vehicles")
    public ResponseEntity<List<HarmonizedVehicleOffer>> searchVehicles(
            @RequestParam String pickupCity,
            @RequestParam(required = false) String dropoffCity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate rentalStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime pickupTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate rentalEnd,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime dropoffTime,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean withDriver,
            @RequestParam(defaultValue = "true") boolean driverAge25Plus,
            @RequestParam(defaultValue = "XAF") String currency
    ) {
        return ResponseEntity.ok(vehicleSearchService.search(new VehicleSearchCriteria(
                pickupCity, dropoffCity, rentalStart, pickupTime, rentalEnd, dropoffTime,
                category, withDriver, driverAge25Plus, currency
        )));
    }

    @GetMapping("/properties")
    public ResponseEntity<List<HarmonizedPropertyOffer>> searchProperties(@Valid @ModelAttribute PropertySearchRequest request) {
        return ResponseEntity.ok(propertySearchService.search(request.toCriteria()));
    }
}