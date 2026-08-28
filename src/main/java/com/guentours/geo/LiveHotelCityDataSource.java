package com.guentours.geo;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Primary
public class LiveHotelCityDataSource implements HotelCityDataSource {

    private static final Logger log = LoggerFactory.getLogger(LiveHotelCityDataSource.class);

    private static final int DEFAULT_MAX_PAGES = 50;
    private static final int ABSOLUTE_MAX_PAGES = 500;

    private final RestClient restClient;

    @Value("${travelopro.user_id:cscreativ_testAPI}")
    private String userId;

    @Value("${travelopro.user_password:cscreativTest@2026}")
    private String userPassword;

    @Value("${travelopro.ip_address:129.0.60.181}")
    private String ipAddress;

    @Value("${travelopro.access:Test}")
    private String access;

    public LiveHotelCityDataSource(RestClient.Builder restClientBuilder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(15000);
        requestFactory.setReadTimeout(15000);

        this.restClient = restClientBuilder
                .baseUrl("https://travelnext.works")
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public List<HotelCityRecord> fetchAll() {
        log.info("🏨 Début de la récupération complète des villes hôtelières...");
        log.info("Configuration active -> User: {} | Access: {} | IP: {}", userId, access, ipAddress);

        List<HotelCityRecord> allCities = new ArrayList<>();

        int limit = 10000;
        long delayMs = 500;

        int from = 1;
        int to = limit;
        int page = 1;
        int consecutiveFailures = 0;
        int maxPages = DEFAULT_MAX_PAGES; // filet de sécurité par défaut, ajusté dès que total_count est connu
        boolean totalCountApplied = false;

        while (true) {
            if (page > maxPages) {
                log.error("❌ Sécurité : Nombre maximum de pages atteint ({}).", maxPages);
                break;
            }

            if ((page > 1 || consecutiveFailures > 0) && delayMs > 0) {
                try {
                    TimeUnit.MILLISECONDS.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Sync interrompue pendant le délai anti-rate limit.");
                    break;
                }
            }

            HotelCityApiResponse response = fetchPage(from, to);

            if (response == null || response.cities() == null) {
                consecutiveFailures++;
                if (consecutiveFailures >= 3) {
                    log.error("❌ 3 échecs consécutifs à la page {}. Arrêt du flux.", page);
                    break;
                }
                log.warn("⚠️ Erreur sur la plage [{}→{}], nouvelle tentative...", from, to);
                continue;
            }

            List<HotelCityApiResponse.CityItem> remoteCities = response.cities();

            if (remoteCities.isEmpty()) {
                log.info("✓ Fin des données atteinte à la page {}. Total récolté : {} villes.", page - 1, allCities.size());
                break;
            }

            consecutiveFailures = 0;

            if (!totalCountApplied) {
                totalCountApplied = true;
                Integer totalCount = parseTotalCount(response.totalCount());
                if (totalCount != null && totalCount > 0) {
                    int computedMaxPages = (int) Math.ceil(totalCount / (double) limit) + 1;
                    maxPages = Math.min(Math.max(computedMaxPages, page), ABSOLUTE_MAX_PAGES);
                    log.info("ℹ️ total_count annoncé par l'API : {} → limite ajustée à {} page(s).", totalCount, maxPages);
                }
            }

            log.info("📡 Page {} : Index {} à {} reçus (+{} villes).", page, from, to, remoteCities.size());

            int skipped = 0;
            for (HotelCityApiResponse.CityItem item : remoteCities) {
                try {
                    allCities.add(new HotelCityRecord(
                            item.cityName() != null ? item.cityName().trim() : "",
                            item.countryName() != null ? item.countryName().trim() : "",
                            Double.parseDouble(item.latitude()),
                            Double.parseDouble(item.longitude())
                    ));
                } catch (Exception e) {
                    skipped++;
                    log.warn("⚠️ Ville ignorée (id={}, city={}) : coordonnées invalides ({})", item.id(), item.cityName(), e.getMessage());
                }
            }
            if (skipped > 0) {
                log.warn("⚠️ {} ville(s) ignorée(s) sur la page {} pour coordonnées invalides.", skipped, page);
            }

            from = to + 1;
            to = from + limit - 1;
            page++;
        }

        return allCities;
    }

    private Integer parseTotalCount(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private HotelCityApiResponse fetchPage(int from, int to) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/hotel-api-v6/cities")
                            .queryParam("from", from)
                            .queryParam("to", to)
                            .queryParam("user_id", userId)
                            .queryParam("user_password", userPassword)
                            .queryParam("ip_address", ipAddress)
                            .queryParam("access", access)
                            .build())
                    .retrieve()
                    .body(HotelCityApiResponse.class);

        } catch (Exception e) {
            log.error("❌ Erreur de communication sur l'index [{}→{}] : {}", from, to, e.getMessage());
            return null;
        }
    }
}

/**
 * Enveloppe globale de la réponse renvoyée par l'API.
 */
record HotelCityApiResponse(
        @JsonProperty("total_count") String totalCount,
        List<CityItem> cities
) {
    public record CityItem(
            int id,
            @JsonProperty("city_name") String cityName,
            @JsonProperty("country_name") String countryName,
            @JsonProperty("country_code") String countryCode,
            String latitude,
            String longitude
    ) {}
}