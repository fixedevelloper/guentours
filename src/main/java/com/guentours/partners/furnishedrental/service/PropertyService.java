package com.guentours.partners.furnishedrental.service;

import com.guentours.partners.furnishedrental.domain.Property;
import com.guentours.partners.furnishedrental.domain.PropertyAvailability;
import com.guentours.partners.furnishedrental.repository.PropertyAvailabilityRepository;
import com.guentours.partners.furnishedrental.repository.PropertyRepository;
import com.guentours.partners.furnishedrental.web.PropertyAvailabilityRequest;
import com.guentours.partners.furnishedrental.web.PropertyRegistrationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyAvailabilityRepository availabilityRepository;

    public PropertyService(PropertyRepository propertyRepository,
                            PropertyAvailabilityRepository availabilityRepository) {
        this.propertyRepository = propertyRepository;
        this.availabilityRepository = availabilityRepository;
    }

    @Transactional
    public Property create(String partnerId, PropertyRegistrationRequest req) {
        Property property = new Property(
                partnerId, req.title(), req.propertyType(), req.address(), req.city(), req.country(),
                req.bedrooms(), req.bathrooms(), req.maxGuests(), req.amenities(),
                req.pricePerNight(), req.currency(), req.minStayNights(), req.description()
        );
        return propertyRepository.save(property);
    }

    public Page<Property> findByPartner(String partnerId, Pageable pageable) {
        return propertyRepository.findByPartnerId(partnerId, pageable);
    }

    public Property findById(String id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Logement introuvable"));
    }

    @Transactional
    public void suspend(String id) { findById(id).suspend(); }

    @Transactional
    public void activate(String id) { findById(id).activate(); }

    @Transactional
    public void delete(String id) {
        if (!propertyRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Logement introuvable");
        }
        propertyRepository.deleteById(id);
    }

    @Transactional
    public PropertyAvailability upsertAvailability(String propertyId, PropertyAvailabilityRequest req) {
        Property property = findById(propertyId);

        return availabilityRepository.findByPropertyIdAndStayDate(propertyId, req.stayDate())
                .map(existing -> {
                    availabilityRepository.delete(existing);
                    return availabilityRepository.save(
                            new PropertyAvailability(property, req.stayDate(), req.isAvailable()));
                })
                .orElseGet(() -> availabilityRepository.save(
                        new PropertyAvailability(property, req.stayDate(), req.isAvailable())));
    }

    public List<PropertyAvailability> getAvailability(String propertyId, LocalDate from, LocalDate to) {
        return availabilityRepository.findByPropertyIdAndStayDateBetween(propertyId, from, to);
    }
}
