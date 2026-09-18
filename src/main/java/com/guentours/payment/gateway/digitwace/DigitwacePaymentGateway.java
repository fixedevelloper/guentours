package com.guentours.payment.gateway.digitwace;

import com.guentours.payment.gateway.ChargeRequest;
import com.guentours.payment.gateway.ChargeResult;
import com.guentours.payment.gateway.ChargeStatus;
import com.guentours.payment.gateway.PaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Mobile wallet payin via WacePay PayIn (Digitwace) - see
 * https://docs.digitwace.com/docs/wacepay-payin/get-started. Push-to-wallet like Flutterwave's
 * mobile money flow (the payer confirms on their phone), so a charge always comes back PENDING;
 * the final outcome arrives via {@code DigitwaceWebhookController} (with {@link
 * DigitwaceStatusClient} re-verifying before it's trusted). Request/response field names in {@link
 * DigitwaceInitPaymentRequest}/{@link DigitwaceInitPaymentResponse} are best-effort guesses pending
 * WacePay's real Postman collection/OpenAPI spec - see {@link DigitwaceProperties}.
 */
@Slf4j
@Component("DIGITWACE")
@Profile("!test")
public class DigitwacePaymentGateway implements PaymentGateway {

    private final RestClient restClient;
    private final DigitwaceTokenClient tokenClient;
    private final DigitwaceProperties properties;

    public DigitwacePaymentGateway(RestClient.Builder restClientBuilder, DigitwaceTokenClient tokenClient,
                                   DigitwaceProperties properties) {
        this.properties = properties;
        this.tokenClient = tokenClient;
        this.restClient = restClientBuilder.baseUrl(properties.baseUrlOrDefault()).build();
    }

    @Override
    public ChargeResult charge(ChargeRequest request) {
        String currency = request.countryCurrency() != null ? request.countryCurrency() : request.currency();
        DigitwaceInitPaymentRequest body = new DigitwaceInitPaymentRequest(
                request.paymentReference(), request.amount(), currency, request.countryCode(),
                request.mobileNumber(), request.customerEmail(),
                "Paiement GuenTours - " + request.paymentReference());

        DigitwaceInitPaymentResponse response;
        try {
            response = restClient.post()
                    .uri(properties.initPaymentPathOrDefault())
                    .header("Authorization", "Bearer " + tokenClient.bearerToken())
                    .body(body)
                    .retrieve()
                    .body(DigitwaceInitPaymentResponse.class);
        } catch (Exception e) {
            log.error("Erreur WacePay PayIn lors de l'initialisation du paiement {}", request.paymentReference(), e);
            return ChargeResult.declined("Erreur de communication avec WacePay PayIn : " + e.getMessage());
        }

        if (response == null || response.transactionId() == null) {
            log.error("Réponse WacePay PayIn vide/incomplète pour le paiement {}", request.paymentReference());
            return ChargeResult.declined("Réponse WacePay PayIn inattendue (transaction manquante)");
        }

        log.info("WacePay PayIn init-payment pour {} : transactionId={}, status déclaré={}",
                request.paymentReference(), response.transactionId(), response.status());

        ChargeStatus status = DigitwaceStatusMapper.map(response.status());
        if (status == ChargeStatus.FAILED) {
            return ChargeResult.declined(
                    response.message() != null ? response.message() : "Paiement refusé par WacePay PayIn");
        }
        if (status == ChargeStatus.SUCCEEDED) {
            return ChargeResult.success(response.transactionId(), request.payerReferenceLast4());
        }
        return new ChargeResult(ChargeStatus.PENDING, response.transactionId(), request.payerReferenceLast4(), null, null);
    }
}
