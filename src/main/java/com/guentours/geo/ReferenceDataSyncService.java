package com.guentours.geo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Reloads the airports/hotel_cities reference tables from their configured
 * {@link AirportDataSource}/{@link HotelCityDataSource}.
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

    // =========================================================================
    // AIRPORTS
    // =========================================================================

    /**
     * Exécuté par le Scheduler au démarrage : vérifie la BDD avant d'importer.
     */
    @Transactional
    public int syncAirports() {
        long existingCount = airportRepository.count();
        if (existingCount > 0) {
            log.info("Aéroports déjà présents en BDD ({} enregistrements). Synchronisation automatique ignorée.", existingCount);
            return (int) existingCount;
        }
        return forceSyncAirports();
    }

    /**
     * Force la synchronisation des aéroports (appelé si la BDD est vide ou par l'AdminController).
     */
    @Transactional
    public int forceSyncAirports() {
        log.info("Démarrage du chargement des aéroports depuis la source...");
        List<AirportRecord> records = airportDataSource.fetchAll();
        List<Airport> airports = records.stream()
                .map(r -> new Airport(r.airportCode(), r.airportName(), r.city(), r.country()))
                .toList();
        airportRepository.saveAll(airports);
        log.info("Synced {} airports", airports.size());
        return airports.size();
    }

    // =========================================================================
    // HOTEL CITIES
    // =========================================================================

    /**
     * Exécuté par le Scheduler au démarrage : vérifie la BDD avant d'importer.
     */
    @Transactional
    public int syncHotelCities() {
        long existingCount = hotelCityRepository.count();
        if (existingCount > 0) {
            log.info("Villes d'hôtels déjà présentes en BDD ({} enregistrements). Synchronisation automatique ignorée.", existingCount);
            return (int) existingCount;
        }
        return forceSyncHotelCities();
    }

    /**
     * Force la synchronisation des villes d'hôtels (appelé si la BDD est vide ou par l'AdminController).
     */
    @Transactional
    public int forceSyncHotelCities() {
        log.info("Démarrage du chargement des villes d'hôtels depuis la source...");
        List<HotelCityRecord> records = hotelCityDataSource.fetchAll();
        log.info("Données reçues de la source : {} enregistrements", records.size());

        if (records == null || records.isEmpty()) {
            return 0;
        }

        Pattern diacriticsPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

        // 1. Charger les villes déjà existantes en BDD (évite l'erreur INSERT sur clé unique lors d'une re-synchro)
        Map<String, HotelCity> existingDbCities = hotelCityRepository.findAll().stream()
                .collect(Collectors.toMap(
                        c -> buildNormalizedKey(c.getCityName(), c.getCountryName(), diacriticsPattern),
                        c -> c,
                        (existing, replacement) -> existing
                ));

        // 2. Traiter le flux : dédoublonner en mémoire + fusionner avec les enregistrements BDD
        Map<String, HotelCity> citiesToSave = new HashMap<>();

        for (HotelCityRecord r : records) {
            if (r.cityName() == null || r.countryName() == null) continue;

            String key = buildNormalizedKey(r.cityName(), r.countryName(), diacriticsPattern);

            // Vérifier si la ville existe déjà en BDD ou si elle a déjà été lue dans la boucle
            HotelCity cityEntity = existingDbCities.get(key);
            if (cityEntity == null) {
                cityEntity = citiesToSave.get(key);
            }

            if (cityEntity == null) {
                // Nouvelle ville -> Nouvelle entité (INSERT)
                cityEntity = new HotelCity(
                        r.cityName().trim(),
                        r.countryName().trim(),
                        r.latitude(),
                        r.longitude()
                );
            } else {
                // Ville existante -> On conserve l'ID JPA et on met à jour les coordonnées (UPDATE)
                cityEntity.setLatitude(r.latitude());
                cityEntity.setLongitude(r.longitude());
            }

            citiesToSave.put(key, cityEntity);
        }

        List<HotelCity> saved = hotelCityRepository.saveAll(citiesToSave.values());
        log.info("Synced {} hotel cities", saved.size());

        return saved.size();
    }

    /**
     * Clé de fusion INSENSIBLE aux accents, à la casse et aux équivalences MySQL (ex: 'ß' -> 'ss').
     */
    private String buildNormalizedKey(String cityName, String countryName, Pattern diacriticsPattern) {
        String rawKey = (cityName.trim() + "-" + countryName.trim())
                .toLowerCase()
                .replace("ß", "ss")
                .replace("đ", "d")
                .replace("æ", "ae")
                .replace("ø", "o");

        String normalized = Normalizer.normalize(rawKey, Normalizer.Form.NFD);
        return diacriticsPattern.matcher(normalized).replaceAll("");
    }
    /**
     * Utilitaire pour filtrer un Stream sur une clé spécifique
     */
    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}