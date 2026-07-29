package com.guentours.partners.furnishedrental.web;

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
//@PreAuthorize("hasRole('PARTNER_FURNISHED_RENTAL') and #partnerId == authentication.principal.partnerId")
public class PropertyController {

    private final PropertyService service;

    public PropertyController(PropertyService service) {
        this.service = service;
    }

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
    public void suspend(@PathVariable String propertyId) {
        service.suspend(propertyId);
    }

    @PatchMapping("/{propertyId}/activate")
    public void activate(@PathVariable String propertyId) {
        service.activate(propertyId);
    }

    @DeleteMapping("/{propertyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String propertyId) {
        service.delete(propertyId);
    }

    @PutMapping("/{propertyId}")
    public PropertyResponse update(@PathVariable String propertyId, @Valid @RequestBody PropertyRegistrationRequest req) {
        return PropertyResponse.from(service.update(propertyId, req));
    }

    @PostMapping("/{propertyId}/images")
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyResponse addPropertyImage(@PathVariable String propertyId, @Valid @RequestBody ImageUploadRequest req) {
        return PropertyResponse.from(service.addPropertyImage(propertyId, req));
    }

    @DeleteMapping("/{propertyId}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePropertyImage(@PathVariable String propertyId, @PathVariable String imageId) {
        service.removePropertyImage(propertyId, imageId);
    }

    @PatchMapping("/{propertyId}/images/{imageId}/primary")
    public PropertyResponse setPrimaryPropertyImage(@PathVariable String propertyId, @PathVariable String imageId) {
        return PropertyResponse.from(service.setPrimaryPropertyImage(propertyId, imageId));
    }

    @PutMapping("/{propertyId}/availability")
    public PropertyAvailabilityResponse upsertAvailability(@PathVariable String propertyId,
                                                   @Valid @RequestBody PropertyAvailabilityRequest req) {
        return PropertyAvailabilityResponse.from(service.upsertAvailability(propertyId, req));
    }

    @GetMapping("/{propertyId}/availability")
    public List<PropertyAvailabilityResponse> getAvailability(@PathVariable String propertyId,
                                                      @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return service.getAvailability(propertyId, from, to).stream().map(PropertyAvailabilityResponse::from).toList();
    }
}