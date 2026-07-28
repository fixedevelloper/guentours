package com.guentours.geo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Admin CRUD over the hotel_cities reference table (search/autocomplete lives in GeoSearchService). */
@Service
public class HotelCityAdminService {

    private final HotelCityRepository repository;

    public HotelCityAdminService(HotelCityRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<HotelCity> list(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return repository.findAll(pageable);
        }
        String q = query.trim();
        return repository.findByCityNameContainingIgnoreCaseOrCountryNameContainingIgnoreCase(q, q, pageable);
    }

    @Transactional
    public HotelCity create(HotelCityUpsertRequest req) {
        if (repository.findByCityNameIgnoreCaseAndCountryNameIgnoreCase(req.cityName(), req.countryName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cette ville existe déjà pour ce pays : " + req.cityName() + ", " + req.countryName());
        }
        HotelCity city = new HotelCity(req.cityName().trim(), req.countryName().trim(), req.latitude(), req.longitude());
        return repository.save(city);
    }

    @Transactional
    public HotelCity update(Long id, HotelCityUpsertRequest req) {
        HotelCity city = findById(id);
        if (repository.existsByCityNameIgnoreCaseAndCountryNameIgnoreCaseAndIdNot(req.cityName(), req.countryName(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une autre ville existe déjà pour ce pays : " + req.cityName() + ", " + req.countryName());
        }
        city.refresh(req.cityName().trim(), req.countryName().trim(), req.latitude(), req.longitude());
        return repository.save(city);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ville introuvable : " + id);
        }
        repository.deleteById(id);
    }

    private HotelCity findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ville introuvable : " + id));
    }
}
