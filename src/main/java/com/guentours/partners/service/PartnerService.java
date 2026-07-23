package com.guentours.partners.service;

import com.guentours.partners.domain.Partner;
import com.guentours.partners.domain.PartnerStatus;
import com.guentours.partners.event.PartnerApprovedEvent;
import com.guentours.partners.event.PartnerRegisteredEvent;
import com.guentours.partners.repository.PartnerRepository;
import com.guentours.partners.web.PartnerRegistrationRequest;
import com.guentours.partners.web.PartnerUpdateRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PartnerService {

    private final PartnerRepository repository;
    private final ApplicationEventPublisher events;

    public PartnerService(PartnerRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @Transactional
    public Partner register(PartnerRegistrationRequest req) {
        if (repository.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email déjà utilisé");
        }
        if (repository.existsByRegistrationNumber(req.registrationNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "N° RCCM déjà enregistré");
        }

        Partner partner = new Partner(
                req.partnerType(), req.companyName(), req.registrationNumber(),
                req.contactName(), req.email(), req.phone(), req.city(),
                req.country(), req.fleetOrUnitsCount(), req.description()
        );

        Partner saved = repository.save(partner);

        events.publishEvent(new PartnerRegisteredEvent(saved.getId(), saved.getCompanyName(), saved.getPartnerType()));

        return saved;
    }

    public Page<Partner> findAll(PartnerStatus status, Pageable pageable) {
        if (status != null) {
            return repository.findByStatus(status, pageable);
        }
        return repository.findAll(pageable);
    }

    public Partner findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partenaire introuvable"));
    }

    @Transactional
    public Partner approve(String id) {
        Partner p = findById(id);
        p.approve();
        Partner saved = repository.save(p);

        events.publishEvent(new PartnerApprovedEvent(
                saved.getId(), saved.getEmail(), saved.getCompanyName(),
                saved.getContactName(), saved.getPartnerType()
        ));

        return saved;
    }

    @Transactional
    public Partner reject(String id) {
        Partner p = findById(id);
        p.reject();
        return repository.save(p);
    }
    // PartnerService.java

    @Transactional
    public Partner update(String id, PartnerUpdateRequest req) {
        Partner partner = findById(id);
        partner.updateProfile(req.companyName(), req.phone(), req.city(), req.country(), req.description());
        return repository.save(partner);
    }
}
