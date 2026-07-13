package com.guentours.geo;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Component
@Primary // Indique à Spring d'utiliser cette source dynamique en priorité
class LiveHotelCityDataSource implements HotelCityDataSource {

    private static final Logger log = LoggerFactory.getLogger(LiveHotelCityDataSource.class);

    private final RestClient restClient;
    private final SeedHotelCityDataSource fallbackDataSource;

    public LiveHotelCityDataSource(RestClient.Builder restClientBuilder, SeedHotelCityDataSource fallbackDataSource) {
        this.fallbackDataSource = fallbackDataSource;

        // Configuration sécurisée des timeouts réseau
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10000); // 10 secondes max pour se connecter
        requestFactory.setReadTimeout(30000);    // 30 secondes max pour lire la liste

        this.restClient = restClientBuilder
                .baseUrl("https://travelnext.works")
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public List<HotelCityRecord> fetchAll() {
        log.info("Fetching dynamic hotel cities from TravelNext API...");

        try {
            // Exécution de l'appel GET avec les paramètres d'authentification et de pagination demandés
            HotelCityApiResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/hotel-api-v6/cities")
                            .queryParam("from", 1)
                            .queryParam("to", 100)
                            .queryParam("user_id", "cscreativ_testAPI")
                            .queryParam("user_password", "cscreativTest@2026")
                            .queryParam("ip_address", "129.0.60.181")
                            .queryParam("access", "Test")
                            .build())
                    .retrieve()
                    .body(HotelCityApiResponse.class);

            if (response == null || response.cities() == null || response.cities().isEmpty()) {
                log.warn("TravelNext Hotel API returned no cities. Falling back to seed data.");
                return fallbackDataSource.fetchAll();
            }

            // Transformation et conversion vers vos objets métier internes HotelCityRecord
            return response.cities().stream()
                    .map(city -> new HotelCityRecord(
                            city.cityName(),
                            city.countryName(),
                            Double.parseDouble(city.latitude()),
                            Double.parseDouble(city.longitude())
                    ))
                    .toList();

        } catch (Exception e) {
            log.error("Failed to fetch live hotel cities from TravelNext: {}. Using hardcoded seeds as fallback.", e.getMessage());
            // Résilience : renvoi immédiat des ~50 villes d'origine si l'API externe subit une panne
            return fallbackDataSource.fetchAll();
        }
    }
}

/**
 * Enveloppe globale de la réponse renvoyée par l'API des villes d'hôtels.
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