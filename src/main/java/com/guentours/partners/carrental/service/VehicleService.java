package com.guentours.partners.carrental.service;

import com.guentours.partners.carrental.domain.Transmission;
import com.guentours.partners.carrental.domain.Vehicle;
import com.guentours.partners.carrental.domain.VehicleAvailability;
import com.guentours.partners.carrental.domain.VehicleCategory;
import com.guentours.partners.carrental.repository.VehicleAvailabilityRepository;
import com.guentours.partners.carrental.repository.VehicleRepository;
import com.guentours.partners.carrental.web.VehicleAvailabilityRequest;
import com.guentours.partners.carrental.web.VehicleRegistrationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleAvailabilityRepository availabilityRepository;

    public VehicleService(VehicleRepository vehicleRepository, VehicleAvailabilityRepository availabilityRepository) {
        this.vehicleRepository = vehicleRepository;
        this.availabilityRepository = availabilityRepository;
    }

    // --- Véhicules ---

    @Transactional
    public Vehicle create(String partnerId, VehicleRegistrationRequest req) {
        Vehicle vehicle = new Vehicle(
                partnerId,
                req.brand(),
                req.model(),
                req.year(),
                parseCategory(req.category()),
                parseTransmission(req.transmission()),
                req.seats(),
                req.airConditioning(),
                req.pricePerDay(),
                req.currency(),
                req.unitsCount(),
                req.pickupLocations()
        );
        return vehicleRepository.save(vehicle);
    }

    private VehicleCategory parseCategory(String value) {
        try {
            return VehicleCategory.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Catégorie de véhicule invalide : " + value);
        }
    }

    private Transmission parseTransmission(String value) {
        try {
            return Transmission.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de transmission invalide : " + value);
        }
    }

    @Transactional(readOnly = true)
    public Page<Vehicle> findByPartner(String partnerId, Pageable pageable) {
        return vehicleRepository.findByPartnerId(partnerId, pageable);
    }

    @Transactional(readOnly = true)
    public Vehicle findById(String id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Véhicule introuvable"));
    }

    @Transactional
    public void delete(String id) {
        if (!vehicleRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Véhicule introuvable");
        }
        vehicleRepository.deleteById(id);
    }

    @Transactional
    public void suspend(String id) {
        findById(id).suspend();
    }

    @Transactional
    public void activate(String id) {
        findById(id).activate();
    }

    // --- Disponibilités ---

    @Transactional
    public VehicleAvailability upsertAvailability(String vehicleId, VehicleAvailabilityRequest req) {
        Vehicle vehicle = findById(vehicleId);

        return availabilityRepository.findByVehicle_IdAndRentDateBetween(vehicleId, req.rentDate(), req.rentDate())
                .stream()
                .findFirst()
                .map(existing -> {
                    availabilityRepository.delete(existing);
                    return availabilityRepository.save(new VehicleAvailability(vehicle, req.rentDate(), req.unitsAvailable()));
                })
                .orElseGet(() -> availabilityRepository.save(new VehicleAvailability(vehicle, req.rentDate(), req.unitsAvailable())));
    }

    @Transactional(readOnly = true)
    public List<VehicleAvailability> getAvailability(String vehicleId, LocalDate from, LocalDate to) {
        return availabilityRepository.findByVehicle_IdAndRentDateBetween(vehicleId, from, to);
    }
}