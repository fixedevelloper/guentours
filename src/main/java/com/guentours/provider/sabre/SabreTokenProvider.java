package com.guentours.provider.sabre;

import com.guentours.provider.ProviderProperties;
import com.guentours.shared.exception.ProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * OAuth2 token exchange for Sabre's REST APIs ({@code POST /v3/auth/token}), shared across
 * every Sabre endpoint (search, revalidate, booking, payment). The token is cached in memory
 * and refreshed a little ahead of its real expiry (Sabre tokens are valid for 7 days,
 * {@code expires_in: 604800}) rather than on every call.
 *
 * <p>Matches Sabre's verified v3 contract: the {@code Authorization} header is plain HTTP
 * Basic ({@code base64(clientId:clientSecret)}) and the form body uses the {@code password}
 * grant carrying the agency's Sabre username (e.g. {@code xxxx-DEVCENTER-EXT}) and password -
 * unlike the older v2 endpoint, which used client-credentials with a double-base64 token.
 */
@Slf4j
class SabreTokenProvider {

    private static final long EXPIRY_SAFETY_MARGIN_SECONDS = 60;

    private final RestClient restClient;
    private final ProviderProperties.Vendor config;
    private final String securityToken;

    private volatile String cachedAccessToken;
    private volatile Instant cachedTokenExpiresAt = Instant.EPOCH;

    SabreTokenProvider(RestClient restClient, ProviderProperties.Vendor config) {
        this.restClient = restClient;
        this.config = config;
        this.securityToken = Base64.getEncoder().encodeToString(
                (config.getApiKey() + ":" + config.getApiSecret()).getBytes(StandardCharsets.UTF_8));
    }

    synchronized String getAccessToken() {
        if (cachedAccessToken != null && Instant.now().isBefore(cachedTokenExpiresAt)) {
            return cachedAccessToken;
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("username", config.getUsername());
        body.add("password", config.getPassword());

        String host = config.getBaseUrl().isBlank() ? "https://api.platform.sabre.com" : config.getBaseUrl();
        SabreTokenResponse response;
        try {
            response = restClient.post()
                    .uri("/v3/auth/token")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + securityToken)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(SabreTokenResponse.class);
        } catch (RestClientResponseException ex) {
            // Surface Sabre's real reason instead of a bare "401 [no body]": an empty-body 401
            // means the Basic client-id/secret was rejected (wrong key/secret, or CERT creds sent
            // to the production host and vice-versa - the host defaults to production when
            // SABRE_BASE_URL is blank); a JSON error body (invalid_client/invalid_grant) means the
            // credentials reached Sabre but were refused.
            String detail = ex.getResponseBodyAsString();
            String reason = detail == null || detail.isBlank()
                    ? "empty body - check SABRE_API_KEY/SABRE_API_SECRET and that SABRE_BASE_URL matches the "
                            + "credentials' environment (CERT vs production)"
                    : detail;
            log.error("Sabre auth failed at {}/v3/auth/token: {} {} - {}",
                    host, ex.getStatusCode().value(), ex.getStatusText(), reason);
            throw new ProviderException("Sabre auth failed (" + ex.getStatusCode().value() + " against " + host
                    + "): " + reason);
        }

        if (response == null || response.accessToken() == null) {
            throw new ProviderException("Sabre token exchange returned no access_token");
        }

        cachedAccessToken = response.accessToken();
        cachedTokenExpiresAt = Instant.now().plusSeconds(
                Math.max(response.expiresInSeconds() - EXPIRY_SAFETY_MARGIN_SECONDS, 0));
        return cachedAccessToken;
    }
}
