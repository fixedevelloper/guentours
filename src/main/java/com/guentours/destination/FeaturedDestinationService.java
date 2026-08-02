package com.guentours.destination;

import com.guentours.booking.domain.BookingRepository;
import com.guentours.booking.domain.DestinationBookingCount;
import com.guentours.geo.Airport;
import com.guentours.geo.AirportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/**
 * Backs the homepage's "popular destinations" section. Entries are suggested automatically from
 * real flight booking volume ({@link #refreshFromBookings}), then fully editable by an admin
 * (image, ordering, active flag) - the auto-suggestion only ever adds a destination it hasn't seen
 * before, it never touches an existing row, so admin curation always sticks.
 */
@Service
public class FeaturedDestinationService {

    private static final Logger log = LoggerFactory.getLogger(FeaturedDestinationService.class);

    private final FeaturedDestinationRepository repository;
    private final BookingRepository bookingRepository;
    private final AirportRepository airportRepository;

    public FeaturedDestinationService(FeaturedDestinationRepository repository, BookingRepository bookingRepository,
                                      AirportRepository airportRepository) {
        this.repository = repository;
        this.bookingRepository = bookingRepository;
        this.airportRepository = airportRepository;
    }

    @Transactional(readOnly = true)
    public List<FeaturedDestination> getActive() {
        return repository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<FeaturedDestination> getAll() {
        return repository.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional
    public FeaturedDestination create(FeaturedDestinationUpsertRequest req) {
        String code = normalizeCode(req.destinationCode());
        if (code != null && repository.findByDestinationCodeIgnoreCase(code).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette destination existe déjà : " + code);
        }
        FeaturedDestination destination = new FeaturedDestination(req.cityName().trim(), req.countryName().trim(),
                code, req.imageUrl(), req.displayOrder(), req.active());
        return repository.save(destination);
    }

    @Transactional
    public FeaturedDestination update(String id, FeaturedDestinationUpsertRequest req) {
        FeaturedDestination destination = findById(id);
        String code = normalizeCode(req.destinationCode());
        if (code != null && repository.existsByDestinationCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une autre destination existe déjà : " + code);
        }
        destination.update(req.cityName().trim(), req.countryName().trim(), code, req.imageUrl(),
                req.displayOrder(), req.active());
        return repository.save(destination);
    }

    @Transactional
    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination introuvable : " + id);
        }
        repository.deleteById(id);
    }

    /**
     * Adds the top {@code topN} flight destinations by real booking volume that aren't already
     * tracked (whether auto-suggested earlier or added by hand) - active by default, no image yet,
     * appended at the end of the display order. Never touches an existing entry, so this is always
     * safe to re-run on a schedule; an admin still needs to add a photo before a newly-suggested
     * destination looks good on the homepage.
     */
    @Transactional
    public int refreshFromBookings(int topN) {
        List<DestinationBookingCount> counts = bookingRepository.countFlightBookingsByDestination(
                PageRequest.of(0, topN));

        int nextOrder = repository.findAllByOrderByDisplayOrderAsc().stream()
                .mapToInt(FeaturedDestination::getDisplayOrder)
                .max().orElse(-1) + 1;

        int added = 0;
        for (DestinationBookingCount count : counts) {
            String code = count.getDestinationCode();
            if (repository.findByDestinationCodeIgnoreCase(code).isPresent()) {
                continue;
            }
            Optional<Airport> airport = airportRepository.findById(code);
            String cityName = airport.map(Airport::getCity).orElse(code);
            String countryName = airport.map(Airport::getCountry).orElse("");
            repository.save(new FeaturedDestination(cityName, countryName, code, null, nextOrder++, true));
            added++;
        }
        log.info("Featured destinations refresh: added {} new destination(s) from booking volume", added);
        return added;
    }

    private FeaturedDestination findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination introuvable : " + id));
    }

    private String normalizeCode(String code) {
        return code == null || code.isBlank() ? null : code.trim().toUpperCase();
    }
}
