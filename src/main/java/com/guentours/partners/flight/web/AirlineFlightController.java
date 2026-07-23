package com.guentours.partners.flight.web;

import com.guentours.partners.flight.domain.FlightAvailability;
import com.guentours.partners.flight.domain.FlightFare;
import com.guentours.partners.flight.service.AirlineFlightService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/partners/{partnerId}/flights")
//@PreAuthorize("hasRole('PARTNER_AIRLINE') and #partnerId == authentication.principal.partnerId")
public class AirlineFlightController {

    private final AirlineFlightService service;

    public AirlineFlightController(AirlineFlightService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FlightResponse create(@PathVariable String partnerId, @Valid @RequestBody FlightRegistrationRequest req) {
        return FlightResponse.from(service.create(partnerId, req));
    }

    @GetMapping
    public Page<FlightResponse> list(@PathVariable String partnerId, Pageable pageable) {
        return service.findByPartner(partnerId, pageable).map(FlightResponse::from);
    }

    @GetMapping("/{flightId}")
    public FlightResponse getOne(@PathVariable String partnerId, @PathVariable String flightId) {
        return FlightResponse.from(service.findById(flightId));
    }

    @PatchMapping("/{flightId}/suspend")
    public void suspend(@PathVariable String partnerId, @PathVariable String flightId) {
        service.suspend(flightId);
    }

    @PatchMapping("/{flightId}/activate")
    public void activate(@PathVariable String partnerId, @PathVariable String flightId) {
        service.activate(flightId);
    }

    @DeleteMapping("/{flightId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String partnerId, @PathVariable String flightId) {
        service.delete(flightId);
    }
    @PostMapping("/{flightId}/fares")
    @ResponseStatus(HttpStatus.CREATED)
    public FareResponse addFare(@PathVariable String flightId, @Valid @RequestBody FareRequest req) {
        return FareResponse.from(service.addFare(flightId, req));
    }

    @GetMapping("/{flightId}/fares")
    public List<FareResponse> getFares(@PathVariable String flightId) {
        return service.getFares(flightId).stream().map(FareResponse::from).toList();
    }

    @PutMapping("/fares/{fareId}/availability")
    public AvailabilityResponse upsertAvailability(@PathVariable String fareId,
                                                   @Valid @RequestBody AvailabilityUpsertRequest req) {
        return AvailabilityResponse.from(service.upsertAvailability(fareId, req));
    }

    @GetMapping("/fares/{fareId}/availability")
    public List<AvailabilityResponse> getAvailability(@PathVariable String fareId,
                                                      @RequestParam LocalDate from,
                                                      @RequestParam LocalDate to) {
        return service.getAvailability(fareId, from, to).stream().map(AvailabilityResponse::from).toList();
    }

    @DeleteMapping("/fares/{fareId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFare(@PathVariable String fareId) { service.deleteFare(fareId); }
}
