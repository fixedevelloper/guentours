package com.guentours.payment.gateway.digitwace;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Obtains and caches the WacePay PayIn bearer token: Digitwace's auth flow is
 * {@code Base64(public_key:private_key)} as a Basic token, exchanged for a Bearer token used on
 * every other call (see https://docs.digitwace.com/docs/wacepay-payin/get-started). Shared (as a
 * Spring singleton) between {@link DigitwacePaymentGateway} and {@link DigitwaceStatusClient} so
 * both reuse the same cached token instead of each fetching their own.
 */
@Slf4j
@Component
class DigitwaceTokenClient {

    private final RestClient restClient;
    private final DigitwaceProperties properties;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile String cachedToken;
    private volatile Instant cachedTokenExpiry = Instant.EPOCH;

    DigitwaceTokenClient(RestClient.Builder restClientBuilder, DigitwaceProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl(properties.baseUrlOrDefault()).build();
    }

    String bearerToken() {
        if (cachedToken != null && Instant.now().isBefore(cachedTokenExpiry)) {
            return cachedToken;
        }
        lock.lock();
        try {
            if (cachedToken != null && Instant.now().isBefore(cachedTokenExpiry)) {
                return cachedToken;
            }
            return fetchToken();
        } finally {
            lock.unlock();
        }
    }

    /** Call after a request comes back 401, in case the token actually expired earlier than {@link DigitwaceProperties#tokenTtlSecondsOrDefault()} assumes. */
    void invalidate() {
        cachedToken = null;
    }

    private String fetchToken() {
        String basicToken = Base64.getEncoder().encodeToString(
                (properties.publicKey() + ":" + properties.privateKey()).getBytes(StandardCharsets.UTF_8));

        DigitwaceTokenResponse response = restClient.get()
                .uri(properties.tokenPathOrDefault())
                .header("Authorization", "Basic " + basicToken)
                .retrieve()
                .body(DigitwaceTokenResponse.class);

        if (response == null || response.token() == null) {
            throw new IllegalStateException("Réponse WacePay PayIn invalide lors de l'obtention du bearer token");
        }

        cachedToken = response.token();
        cachedTokenExpiry = Instant.now().plusSeconds(Math.max(30, properties.tokenTtlSecondsOrDefault() - 30));
        log.info("Nouveau bearer token WacePay PayIn obtenu (expire ~{})", cachedTokenExpiry);
        return cachedToken;
    }

    /** Field name ("token") is a best-effort guess pending the real schema - see {@link DigitwaceProperties}. */
    private record DigitwaceTokenResponse(String token) {}
}
