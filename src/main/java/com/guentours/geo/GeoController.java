package com.guentours.geo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public autocomplete search for airports (flights) and hotel cities, requires 2+ characters. */
@RestController
public class GeoController {

    private final GeoSearchService geoSearchService;

    public GeoController(GeoSearchService geoSearchService) {
        this.geoSearchService = geoSearchService;
    }

    @GetMapping("/api/geo/airports")
    public List<AirportResponse> searchAirports(@RequestParam("q") String query,
                                                 @RequestParam(value = "limit", required = false) Integer limit) {
        return geoSearchService.searchAirports(query, limit);
    }

    @GetMapping("/api/geo/cities")
    public List<HotelCityResponse> searchCities(@RequestParam("q") String query,
                                                 @RequestParam(value = "limit", required = false) Integer limit) {
        return geoSearchService.searchCities(query, limit);
    }
}
