package com.guentours.partners.furnishedrental.web;

import com.guentours.partners.furnishedrental.domain.Property;
import com.guentours.partners.furnishedrental.domain.PropertyAvailability;
import com.guentours.partners.furnishedrental.service.PropertyService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/partners/{partnerId}/properties")
@PreAuthorize("hasRole('PARTNER_FURNISHED_RENTAL') and #partnerId == authentication.principal.partnerId")
public class PropertyController {

    private final PropertyService service;

    public PropertyController(PropertyService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyResponse create(@PathVariable String partnerId, @Valid @RequestBody PropertyRegistrationRequest req) {
        return PropertyResponse.from(service.create(partnerId, req));
    }

    @GetMapping
    public Page<PropertyResponse> list(@PathVariable String partnerId, Pageable pageable) {
        return service.findByPartner(partnerId, pageable).map(PropertyResponse::from);
    }

    @GetMapping("/{propertyId}")
    public PropertyResponse getOne(@PathVariable String propertyId) {
        return PropertyResponse.from(service.findById(propertyId));
    }

    @PatchMapping("/{propertyId}/suspend")
    public void suspend(@PathVariable String propertyId) { service.suspend(propertyId); }

    @PatchMapping("/{propertyId}/activate")
    public void activate(@PathVariable String propertyId) { service.activate(propertyId); }

    @DeleteMapping("/{propertyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String propertyId) { service.delete(propertyId); }

    @PutMapping("/{propertyId}/availability")
    public PropertyAvailability upsertAvailability(@PathVariable String propertyId,
                                                    @Valid @RequestBody PropertyAvailabilityRequest req) {
        return service.upsertAvailability(propertyId, req);
    }

    @GetMapping("/{propertyId}/availability")
    public List<PropertyAvailability> getAvailability(@PathVariable String propertyId,
                                                       @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return service.getAvailability(propertyId, from, to);
    }
}
