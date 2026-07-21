package com.guentours.partner.web;


import com.guentours.partner.domain.Partner;
import com.guentours.partner.domain.PartnerStatus;
import com.guentours.partner.service.PartnerService;
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

    // Public — appelé depuis le formulaire "Devenir hôte"
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public PartnerResponse register(@Valid @RequestBody PartnerRegistrationRequest req) {
        Partner partner = service.register(req);
        return PartnerResponse.from(partner);
    }

    // Admin — liste paginée pour la revue back-office
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<PartnerResponse> list(@RequestParam(required = false) PartnerStatus status, Pageable pageable) {
        // filtrer via repository.findByStatus si status != null, sinon findAll
        return service.findAll(status, pageable).map(PartnerResponse::from);
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
}
