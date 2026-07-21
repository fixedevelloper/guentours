package com.guentours.partner.service;


import com.guentours.partner.domain.Partner;
import com.guentours.partner.event.PartnerApprovedEvent;
import com.guentours.partner.event.PartnerRegisteredEvent;
import com.guentours.partner.domain.PartnerRepository;
import com.guentours.partner.web.PartnerRegistrationRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

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

        // Publié pour le module admin/notification (email au back-office, etc.)
        events.publishEvent(new PartnerRegisteredEvent(saved.getId(), saved.getCompanyName(), saved.getPartnerType()));

        return saved;
    }

    @Transactional
    public Partner reject(String id) {
        Partner p = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partenaire introuvable"));
        p.reject();
        return repository.save(p);
    }
    @Transactional
    public Partner approve(String id) {
        Partner p = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partenaire introuvable"));
        p.approve();
        Partner saved = repository.save(p);

        events.publishEvent(new PartnerApprovedEvent(
                saved.getId(), saved.getEmail(), saved.getCompanyName(),
                saved.getContactName(), saved.getPartnerType()
        ));

        return saved;
    }
}
