package com.guentours.geo;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@Primary // Indique à Spring d'utiliser cette implémentation dynamique par défaut
class LiveAirportDataSource implements AirportDataSource {

    private static final Logger log = LoggerFactory.getLogger(LiveAirportDataSource.class);

    private final RestClient restClient;
    private final SeedAirportDataSource fallbackDataSource;

    public LiveAirportDataSource(RestClient.Builder restClientBuilder, SeedAirportDataSource fallbackDataSource) {
        this.fallbackDataSource = fallbackDataSource;

        // Configuration des timeouts (10s pour la connexion, 30s pour télécharger la grosse liste)
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10000);
        requestFactory.setReadTimeout(30000);

        this.restClient = restClientBuilder
                .baseUrl("https://travelnext.works")
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public List<AirportRecord> fetchAll() {
        log.info("Fetching dynamic airport list from TravelNext API...");

        AirportApiRequest body = new AirportApiRequest(
                "cscreativ_testAPI",
                "cscreativTest@2026",
                "Test",
                "129.0.60.56"
        );

        try {
            // L'API retournant un tableau JSON direct `[...]`, on le récupère sous forme de tableau Java
            AirportApiResponse[] response = restClient.post()
                    .uri("/api/aeroVE5/airport_list")
                    .body(body)
                    .retrieve()
                    .body(AirportApiResponse[].class);

            if (response == null || response.length == 0) {
                log.warn("TravelNext API returned an empty airport list. Using seed fallback.");
                return fallbackDataSource.fetchAll();
            }

            // Transformation du format de l'API vers votre format interne AirportRecord
            return Arrays.stream(response)
                    .map(api -> new AirportRecord(
                            api.airportCode(),
                            api.airportName(),
                            api.city(),
                            api.country()
                    ))
                    .toList();

        } catch (Exception e) {
            log.error("Failed to fetch live airports from TravelNext: {}. Falling back to hardcoded seeds.", e.getMessage());
            // En cas de coupure réseau ou crash de l'API, l'application continue de fonctionner avec vos 60 aéroports de base
            return fallbackDataSource.fetchAll();
        }
    }
}

/**
 * Payload envoyé pour s'authentifier auprès de l'API TravelNext Aero.
 */
record AirportApiRequest(
        @JsonProperty("user_id") String userId,
        @JsonProperty("user_password") String userPassword,
        String access,
        @JsonProperty("ip_address") String ipAddress
) {}

/**
 * Structure d'un aéroport renvoyé par l'API TravelNext Aero.
 */
record AirportApiResponse(
        @JsonProperty("AirportCode") String airportCode,
        @JsonProperty("AirportName") String airportName,
        @JsonProperty("City") String city,
        @JsonProperty("Country") String country,
        @JsonProperty("CountryCode") String countryCode,
        @JsonProperty("Latitude") String latitude,
        @JsonProperty("Longitude") String longitude
) {}