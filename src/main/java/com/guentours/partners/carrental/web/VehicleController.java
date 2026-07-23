package com.guentours.partners.carrental.web;

import com.guentours.partners.carrental.domain.VehicleAvailability;
import com.guentours.partners.carrental.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/partners/{partnerId}/vehicles")
@PreAuthorize("hasRole('PARTNER_CAR_RENTAL') and #partnerId == authentication.principal.partnerId")
public class VehicleController {

    private final VehicleService service;

    public VehicleController(VehicleService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse create(@PathVariable String partnerId, @Valid @RequestBody VehicleRegistrationRequest req) {
        return VehicleResponse.from(service.create(partnerId, req));
    }

    @GetMapping
    public Page<VehicleResponse> list(@PathVariable String partnerId, Pageable pageable) {
        return service.findByPartner(partnerId, pageable).map(VehicleResponse::from);
    }

    @GetMapping("/{vehicleId}")
    public VehicleResponse getOne(@PathVariable String vehicleId) {
        return VehicleResponse.from(service.findById(vehicleId));
    }

    @PatchMapping("/{vehicleId}/suspend")
    public void suspend(@PathVariable String vehicleId) { service.suspend(vehicleId); }

    @PatchMapping("/{vehicleId}/activate")
    public void activate(@PathVariable String vehicleId) { service.activate(vehicleId); }

    @DeleteMapping("/{vehicleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String vehicleId) { service.delete(vehicleId); }

    @PutMapping("/{vehicleId}/availability")
    public VehicleAvailability upsertAvailability(@PathVariable String vehicleId,
                                                   @Valid @RequestBody VehicleAvailabilityRequest req) {
        return service.upsertAvailability(vehicleId, req);
    }

    @GetMapping("/{vehicleId}/availability")
    public List<VehicleAvailability> getAvailability(@PathVariable String vehicleId,
                                                      @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return service.getAvailability(vehicleId, from, to);
    }
}
