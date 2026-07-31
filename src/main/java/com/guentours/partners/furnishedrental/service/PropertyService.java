package com.guentours.partners.furnishedrental.service;

import com.guentours.partners.furnishedrental.domain.Property;
import com.guentours.partners.furnishedrental.domain.PropertyAvailability;
import com.guentours.partners.furnishedrental.repository.PropertyAvailabilityRepository;
import com.guentours.partners.furnishedrental.repository.PropertyRepository;
import com.guentours.partners.furnishedrental.web.ImageUploadRequest;
import com.guentours.partners.furnishedrental.web.PropertyAvailabilityRequest;
import com.guentours.partners.furnishedrental.web.PropertyRegistrationRequest;
import com.guentours.security.SecurityUtils;
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

    public PropertyService(PropertyRepository propertyRepository, PropertyAvailabilityRepository availabilityRepository) {
        this.propertyRepository = propertyRepository;
        this.availabilityRepository = availabilityRepository;
    }

    // --- Logements ---

    @Transactional
    public Property create(String partnerId, PropertyRegistrationRequest req) {
        SecurityUtils.verifyOwnsPartner(partnerId);
        Property property = new Property(
                partnerId,
                req.title(),
                req.propertyType(),
                req.address(),
                req.city(),
                req.country(),
                req.bedrooms(),
                req.bathrooms(),
                req.maxGuests(),
                req.amenities(),
                req.pricePerNight(),
                req.currency(),
                req.minStayNights(),
                req.description()
        );
        return propertyRepository.save(property);
    }

    @Transactional(readOnly = true)
    public Page<Property> findByPartner(String partnerId, Pageable pageable) {
        SecurityUtils.verifyOwnsPartner(partnerId);
        return propertyRepository.findByPartnerId(partnerId, pageable);
    }

    /** Every other method in this class reaches a {@link Property} through here, so ownership is enforced once. */
    @Transactional(readOnly = true)
    public Property findById(String id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Logement introuvable"));
        SecurityUtils.verifyOwnsPartner(property.getPartnerId());
        return property;
    }

    @Transactional
    public void delete(String id) {
        propertyRepository.delete(findById(id));
    }

    @Transactional
    public void suspend(String id) {
        findById(id).suspend();
    }

    @Transactional
    public void activate(String id) {
        findById(id).activate();
    }

    @Transactional
    public Property update(String id, PropertyRegistrationRequest req) {
        Property property = findById(id);
        property.update(
                req.title(),
                req.propertyType(),
                req.address(),
                req.city(),
                req.country(),
                req.bedrooms(),
                req.bathrooms(),
                req.maxGuests(),
                req.amenities(),
                req.pricePerNight(),
                req.currency(),
                req.minStayNights(),
                req.description()
        );
        return propertyRepository.save(property);
    }

    // --- Galerie d'images ---

    @Transactional
    public Property addPropertyImage(String propertyId, ImageUploadRequest req) {
        Property property = findById(propertyId);
        property.addImage(req.url(), req.caption(), req.displayOrder(), req.isPrimary());
        return propertyRepository.save(property);
    }

    @Transactional
    public void removePropertyImage(String propertyId, String imageId) {
        Property property = findById(propertyId);
        property.removeImage(imageId);
        propertyRepository.save(property);
    }

    @Transactional
    public Property setPrimaryPropertyImage(String propertyId, String imageId) {
        Property property = findById(propertyId);
        boolean exists = property.getImages().stream().anyMatch(img -> img.getId().equals(imageId));
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image introuvable : " + imageId);
        }
        property.setPrimaryImage(imageId);
        return propertyRepository.save(property);
    }

    // --- Disponibilités ---
    @Transactional
    public PropertyAvailability upsertAvailability(String propertyId, PropertyAvailabilityRequest req) {
        Property property = findById(propertyId);

        return availabilityRepository.findByProperty_IdAndStayDateBetween(propertyId, req.stayDate(), req.stayDate())
                .stream()
                .findFirst()
                .map(existing -> {
                    availabilityRepository.delete(existing);
                    return availabilityRepository.save(new PropertyAvailability(property, req.stayDate(), req.isAvailable()));
                })
                .orElseGet(() -> availabilityRepository.save(new PropertyAvailability(property, req.stayDate(), req.isAvailable())));
    }

    @Transactional(readOnly = true)
    public List<PropertyAvailability> getAvailability(String propertyId, LocalDate from, LocalDate to) {
        findById(propertyId);
        return availabilityRepository.findByProperty_IdAndStayDateBetween(propertyId, from, to);
    }
}