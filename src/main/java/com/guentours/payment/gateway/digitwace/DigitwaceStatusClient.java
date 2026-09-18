package com.guentours.payment.gateway.digitwace;

import com.guentours.payment.gateway.ChargeResult;
import com.guentours.payment.gateway.ChargeStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Re-checks a transaction's authoritative status directly with WacePay PayIn. Used by {@code
 * DigitwaceWebhookController} so a forged/garbled webhook body can never confirm a payment on its
 * own - mirrors {@code FlutterwaveWebhookController}'s "never trust the webhook alone"
 * re-verification, which matters even more here since Digitwace's webhook signature scheme (if any)
 * isn't published on its (client-rendered) API reference pages.
 */
@Slf4j
@Component
public class DigitwaceStatusClient {

    private final RestClient restClient;
    private final DigitwaceTokenClient tokenClient;
    private final DigitwaceProperties properties;

    DigitwaceStatusClient(RestClient.Builder restClientBuilder, DigitwaceTokenClient tokenClient,
                          DigitwaceProperties properties) {
        this.properties = properties;
        this.tokenClient = tokenClient;
        this.restClient = restClientBuilder.baseUrl(properties.baseUrlOrDefault()).build();
    }

    public ChargeResult verify(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            return ChargeResult.declined("transactionId manquant, impossible de vérifier le statut WacePay PayIn");
        }

        DigitwaceStatusResponse response;
        try {
            response = restClient.get()
                    .uri(properties.statusPathOrDefault(), transactionId)
                    .header("Authorization", "Bearer " + tokenClient.bearerToken())
                    .retrieve()
                    .body(DigitwaceStatusResponse.class);
        } catch (Exception e) {
            log.error("Erreur WacePay PayIn lors de la vérification du statut de la transaction {}", transactionId, e);
            throw new IllegalStateException("Erreur de communication avec WacePay PayIn : " + e.getMessage(), e);
        }

        if (response == null) {
            throw new IllegalStateException("Réponse WacePay PayIn vide lors de la vérification du statut");
        }

        ChargeStatus status = DigitwaceStatusMapper.map(response.status());
        return switch (status) {
            case SUCCEEDED -> ChargeResult.success(transactionId);
            case FAILED -> ChargeResult.declined(
                    response.message() != null ? response.message() : "Paiement rejeté par WacePay PayIn");
            default -> new ChargeResult(ChargeStatus.PENDING, transactionId, null, null, null);
        };
    }

    /** Field names are a best-effort guess pending WacePay PayIn's real response schema - see {@link DigitwaceProperties}. */
    private record DigitwaceStatusResponse(String status, String message) {}
}
