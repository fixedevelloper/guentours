package com.guentours.geo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Reloads the airports/hotel_cities reference tables from their configured
 * {@link AirportDataSource}/{@link HotelCityDataSource}. Airports upsert cleanly via their
 * natural key (the IATA code is the primary key, so {@code save()} updates in place);
 * hotel cities have a generated id, so existing rows are looked up by (city, country) and
 * refreshed in place instead of being duplicated on every sync.
 */
@Service
public class ReferenceDataSyncService {

    private static final Logger log = LoggerFactory.getLogger(ReferenceDataSyncService.class);

    private final AirportDataSource airportDataSource;
    private final HotelCityDataSource hotelCityDataSource;
    private final AirportRepository airportRepository;
    private final HotelCityRepository hotelCityRepository;

    public ReferenceDataSyncService(AirportDataSource airportDataSource, HotelCityDataSource hotelCityDataSource,
                                     AirportRepository airportRepository, HotelCityRepository hotelCityRepository) {
        this.airportDataSource = airportDataSource;
        this.hotelCityDataSource = hotelCityDataSource;
        this.airportRepository = airportRepository;
        this.hotelCityRepository = hotelCityRepository;
    }

    @Transactional
    public int syncAirports() {
        List<AirportRecord> records = airportDataSource.fetchAll();
        List<Airport> airports = records.stream()
                .map(r -> new Airport(r.airportCode(), r.airportName(), r.city(), r.country()))
                .toList();
        airportRepository.saveAll(airports);
        log.info("Synced {} airports", airports.size());
        return airports.size();
    }

    @Transactional
    public int syncHotelCities() {
        List<HotelCityRecord> records = hotelCityDataSource.fetchAll();
        log.warn("Données reçues de la source : {}", records.size());

        if (!records.isEmpty()) {
            // Pattern pour enlever les marques d'accents combinées
            Pattern diacriticsPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

            List<HotelCity> newCities = records.stream()
                    .filter(r -> r.cityName() != null && r.countryName() != null)
                    .collect(Collectors.toMap(
                            // Clé de fusion INSENSIBLE aux accents et à la casse (Copie le comportement MySQL)
                            r -> {
                                String rawKey = (r.cityName().trim() + "-" + r.countryName().trim()).toLowerCase();
                                // Décompose les caractères (ex: á -> a + ´)
                                String normalized = Normalizer.normalize(rawKey, Normalizer.Form.NFD);
                                // Supprime les accents isolés et remplace les caractères spécifiques comme le 'đ' vietnamien
                                return diacriticsPattern.matcher(normalized)
                                        .replaceAll("")
                                        .replace("đ", "d");
                            },
                            // Valeur : On conserve l'entité avec ses vrais accents d'origine pour l'affichage !
                            r -> new HotelCity(
                                    r.cityName().trim(),
                                    r.countryName().trim(),
                                    r.latitude(),
                                    r.longitude()
                            ),
                            // Si MySQL considère que c'est un doublon, on garde la première version
                            (existing, replacement) -> existing
                    ))
                    .values()
                    .stream()
                    .toList();

            log.info("Nombre de villes uniques après dédoublonnage insensible aux accents : {}", newCities.size());

           // hotelCityRepository.deleteAllInBatch();
            hotelCityRepository.saveAll(newCities);
        }

        log.info("Synced {} hotel cities", records.size());
        return records.size();
    }

    /**
     * Utilitaire pour filtrer un Stream sur une clé spécifique
     */
    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
