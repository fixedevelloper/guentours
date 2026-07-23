package com.guentours.partners.carrental.service;

import com.guentours.partners.carrental.domain.*;
import com.guentours.partners.carrental.repository.*;
import com.guentours.partners.carrental.web.*;
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

    @Transactional
    public Vehicle create(String partnerId, VehicleRegistrationRequest req) {
        Vehicle vehicle = new Vehicle(
                partnerId, req.brand(), req.model(), req.year(),
                VehicleCategory.valueOf(req.category()), Transmission.valueOf(req.transmission()),
                req.seats(), req.airConditioning(), req.pricePerDay(), req.currency(),
                req.unitsCount(), req.pickupLocations()
        );
        return vehicleRepository.save(vehicle);
    }

    public Page<Vehicle> findByPartner(String partnerId, Pageable pageable) {
        return vehicleRepository.findByPartnerId(partnerId, pageable);
    }

    public Vehicle findById(String id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Véhicule introuvable"));
    }

    @Transactional
    public void suspend(String id) { findById(id).suspend(); }

    @Transactional
    public void activate(String id) { findById(id).activate(); }

    @Transactional
    public void delete(String id) {
        if (!vehicleRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Véhicule introuvable");
        }
        vehicleRepository.deleteById(id);
    }

    @Transactional
    public VehicleAvailability upsertAvailability(String vehicleId, VehicleAvailabilityRequest req) {
        Vehicle vehicle = findById(vehicleId);

        return availabilityRepository.findByVehicleIdAndRentDate(vehicleId, req.rentDate())
                .map(existing -> {
                    availabilityRepository.delete(existing);
                    return availabilityRepository.save(new VehicleAvailability(vehicle, req.rentDate(), req.unitsAvailable()));
                })
                .orElseGet(() -> availabilityRepository.save(new VehicleAvailability(vehicle, req.rentDate(), req.unitsAvailable())));
    }

    public List<VehicleAvailability> getAvailability(String vehicleId, LocalDate from, LocalDate to) {
        return availabilityRepository.findByVehicleIdAndRentDateBetween(vehicleId, from, to);
    }
}
