package com.guentours.partners.web;

import com.guentours.partners.domain.Partner;
import com.guentours.partners.domain.PartnerStatus;
import com.guentours.partners.service.PartnerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/partners")
public class PartnerController {

    private final PartnerService service;

    public PartnerController(PartnerService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public PartnerResponse register(@Valid @RequestBody PartnerRegistrationRequest req) {
        Partner partner = service.register(req);
        return PartnerResponse.from(partner);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<PartnerResponse> list(@RequestParam(required = false) PartnerStatus status, Pageable pageable) {
        return service.findAll(status, pageable).map(PartnerResponse::from);
    }

    @GetMapping("/{id}")
    //@PreAuthorize("hasRole('ADMIN')")
    public PartnerResponse getOne(@PathVariable String id) {
        return PartnerResponse.from(service.findById(id));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public PartnerResponse approve(@PathVariable String id) {
        return PartnerResponse.from(service.approve(id));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public PartnerResponse reject(@PathVariable String id) {
        return PartnerResponse.from(service.reject(id));
    }
    @PatchMapping("/{id}")
    @PreAuthorize("#id == authentication.principal.partnerId")
    public PartnerResponse update(@PathVariable String id, @RequestBody PartnerUpdateRequest req) {
        return PartnerResponse.from(service.update(id, req));
    }
}
