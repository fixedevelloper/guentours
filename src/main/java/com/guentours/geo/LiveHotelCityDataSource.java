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

        while (true) {
            if (page > 3) {
                log.error("❌ Sécurité : Nombre maximum de pages atteint.");
                break;
            }

            if (page > 1 && delayMs > 0) {
                try {
                    TimeUnit.MILLISECONDS.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Sync interrompue pendant le délai anti-rate limit.");
                    break;
                }
            }

            List<HotelCityApiResponse.CityItem> remoteCities = fetchPage(from, to);

            if (remoteCities == null) {
                consecutiveFailures++;
                if (consecutiveFailures >= 3) {
                    log.error("❌ 3 échecs consécutifs à la page {}. Arrêt du flux.", page);
                    break;
                }
                log.warn("⚠️ Erreur sur la plage [{}→{}], nouvelle tentative...", from, to);
                continue;
            }

            if (remoteCities.isEmpty()) {
                log.info("✓ Fin des données atteinte à la page {}. Total récolté : {} villes.", page - 1, allCities.size());
                break;
            }

            consecutiveFailures = 0;

            try {
                log.info("📡 Page {} : Index {} à {} reçus (+{} villes).", page, from, to, remoteCities.size());

                for (HotelCityApiResponse.CityItem item : remoteCities) {
                    allCities.add(new HotelCityRecord(
                            // item.id(), // Si ton Record accepte l'ID, sinon adapte selon ton constructeur
                            item.cityName() != null ? item.cityName().trim() : "",
                            item.countryName() != null ? item.countryName().trim() : "",
                            //  item.countryCode() != null ? item.countryCode().trim() : "",
                            Double.parseDouble(item.latitude()),
                            Double.parseDouble(item.longitude())
                    ));
                }

            } catch (Exception e) {
                log.error("❌ Échec de la transformation du lot [{}→{}]: {}", from, to, e.getMessage());
            }

            from = to + 1;
            to = from + limit - 1;
            page++;
        }

        return allCities;
    }

    private List<HotelCityApiResponse.CityItem> fetchPage(int from, int to) {
        try {
            HotelCityApiResponse response = restClient.get()
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

            return response != null ? response.cities() : null;

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