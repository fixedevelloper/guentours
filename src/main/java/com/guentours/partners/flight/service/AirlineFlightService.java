package com.guentours.partners.flight.service;

import com.guentours.partners.flight.domain.*;
import com.guentours.partners.flight.repository.*;
import com.guentours.partners.flight.web.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class AirlineFlightService {

    private final AirlineFlightRepository flightRepository;
    private final FlightFareRepository fareRepository;
    private final FlightAvailabilityRepository availabilityRepository;

    public AirlineFlightService(AirlineFlightRepository flightRepository,
                                 FlightFareRepository fareRepository,
                                 FlightAvailabilityRepository availabilityRepository) {
        this.flightRepository = flightRepository;
        this.fareRepository = fareRepository;
        this.availabilityRepository = availabilityRepository;
    }

    @Transactional
    public AirlineFlight create(String partnerId, FlightRegistrationRequest req) {
        if (flightRepository.existsByPartnerIdAndFlightNumber(partnerId, req.flightNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce numéro de vol existe déjà");
        }
        AirlineFlight flight = new AirlineFlight(
                partnerId, req.flightNumber(), req.aircraftType(),
                req.originAirportCode(), req.destinationAirportCode(),
                req.departureTime(), req.arrivalTime(), req.durationMinutes(), req.operatingDays()
        );
        return flightRepository.save(flight);
    }

    public Page<AirlineFlight> findByPartner(String partnerId, Pageable pageable) {
        return flightRepository.findByPartnerId(partnerId, pageable);
    }

    public AirlineFlight findById(String id) {
        return flightRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vol introuvable"));
    }

    @Transactional
    public void suspend(String id) { findById(id).suspend(); }

    @Transactional
    public void activate(String id) { findById(id).activate(); }

    @Transactional
    public void delete(String id) {
        if (!flightRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vol introuvable");
        }
        flightRepository.deleteById(id);
    }

    @Transactional
    public FlightFare addFare(String flightId, FareRequest req) {
        AirlineFlight flight = findById(flightId);
        FlightFare fare = new FlightFare(
                flight, CabinClass.valueOf(req.cabinClass()), req.basePrice(),
                req.currency(), req.baggageAllowanceKg(), req.totalSeats()
        );
        return fareRepository.save(fare);
    }

    public List<FlightFare> getFares(String flightId) {
        return fareRepository.findByFlightId(flightId);
    }

    @Transactional
    public FlightAvailability upsertAvailability(String fareId, AvailabilityUpsertRequest req) {
        FlightFare fare = fareRepository.findById(fareId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarif introuvable"));

        return availabilityRepository.findByFareIdAndFlightDate(fareId, req.flightDate())
                .map(existing -> {
                    availabilityRepository.delete(existing);
                    return availabilityRepository.save(new FlightAvailability(fare, req.flightDate(), req.seatsAvailable()));
                })
                .orElseGet(() -> availabilityRepository.save(
                        new FlightAvailability(fare, req.flightDate(), req.seatsAvailable())
                ));
    }

    public List<FlightAvailability> getAvailability(String fareId, LocalDate from, LocalDate to) {
        return availabilityRepository.findByFareIdAndFlightDateBetween(fareId, from, to);
    }
    @Transactional
    public void deleteFare(String fareId) {
        if (!fareRepository.existsById(fareId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarif introuvable");
        }
        fareRepository.deleteById(fareId);
    }
}
