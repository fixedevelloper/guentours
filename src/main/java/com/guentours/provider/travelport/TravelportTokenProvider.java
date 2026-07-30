package com.guentours.provider.travelport;

import com.guentours.provider.ProviderProperties;
import com.guentours.shared.exception.ProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;

/**
 * OAuth2 token exchange for Travelport's JSON APIs ({@code POST /oauth/token}), shared across
 * every Travelport endpoint (search, price, reservation). The token is cached in memory and
 * refreshed a little ahead of its real expiry rather than fetched on every call.
 *
 * <p>Travelport serves OAuth on a different host than its APIs (e.g. {@code auth.pp.travelport.net}
 * vs {@code api.pp.travelport.net}), so this uses its own {@link RestClient} pointed at the token
 * host. Client authentication is {@code client_secret_post} (client id/secret in the form body) -
 * real sandbox testing confirmed this against {@code client_secret_basic} (client id/secret as an
 * HTTP Basic Authorization header), which got "invalid_client: Client authentication failed" from
 * Travelport's authorization server every time.
 */
class TravelportTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(TravelportTokenProvider.class);
    private static final long EXPIRY_SAFETY_MARGIN_SECONDS = 60;
    private static final String DEFAULT_TOKEN_URL = "https://auth.pp.travelport.net/oauth/token";

    private final RestClient restClient;
    private final ProviderProperties.Vendor config;
    private final String tokenUrl;

    private volatile String cachedAccessToken;
    private volatile Instant cachedTokenExpiresAt = Instant.EPOCH;

    TravelportTokenProvider(RestClient.Builder restClientBuilder, ProviderProperties.Vendor config) {
        this.config = config;
        this.tokenUrl = config.getTokenUrl().isBlank() ? DEFAULT_TOKEN_URL : config.getTokenUrl();
        this.restClient = restClientBuilder.build();
    }

    synchronized String getAccessToken() {
        if (cachedAccessToken != null && Instant.now().isBefore(cachedTokenExpiresAt)) {
            return cachedAccessToken;
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("username", config.getUsername());
        body.add("password", config.getPassword());
        body.add("client_id", config.getApiKey());
        body.add("client_secret", config.getApiSecret());

        TravelportTokenResponse response;
        try {
            response = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(TravelportTokenResponse.class);
        } catch (RestClientException e) {
            log.error("[Travelport] token exchange failed at {}: {}", tokenUrl, e.getMessage());
            throw new ProviderException("Travelport token exchange failed: " + e.getMessage());
        }

        if (response == null || response.accessToken() == null) {
            throw new ProviderException("Travelport token exchange returned no access_token");
        }

        cachedAccessToken = response.accessToken();
        cachedTokenExpiresAt = Instant.now().plusSeconds(
                Math.max(response.expiresInSeconds() - EXPIRY_SAFETY_MARGIN_SECONDS, 0));
        return cachedAccessToken;
    }
}
