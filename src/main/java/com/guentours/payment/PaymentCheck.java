package com.guentours.payment;

import com.guentours.payment.domain.Payment;
import com.guentours.payment.domain.PaymentRepository;
import com.guentours.payment.domain.PaymentStatus;
import com.guentours.payment.gateway.flutterwave.FlutterwaveProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class PaymentCheck {

    private static final Logger log = LoggerFactory.getLogger(PaymentCheck.class);

    // Un paiement pending plus vieux que ça n'est plus revérifié automatiquement.
    private static final Duration MAX_PENDING_AGE = Duration.ofMinutes(30);

    private final PaymentRepository paymentRepository;
    private final RestClient restClient;

    public PaymentCheck(FlutterwaveProperties properties,
                        PaymentRepository paymentRepository,
                        RestClient.Builder restClientBuilder) {
        this.paymentRepository = paymentRepository;
        this.restClient = restClientBuilder
                .baseUrl("https://api.flutterwave.com/v3")
                .defaultHeader("Authorization", "Bearer " + properties.secretKey())
                .requestFactory(clientHttpRequestFactory())
                .build();
    }

    private static org.springframework.http.client.ClientHttpRequestFactory clientHttpRequestFactory() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(5_000);
        return factory;
    }

    @Scheduled(fixedRate = 20_000, initialDelay = 20_000)
    public void checkPendingPayment() {
        List<Payment> pending = paymentRepository.findByStatus(PaymentStatus.PENDING);

        for (Payment payment : pending) {
            if (isTooOld(payment)) {
                markExpired(payment);
                continue;
            }
            checkOne(payment);
        }
    }

    private void checkOne(Payment payment) {
        try {
            FlutterwaveVerifyResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/transactions/verify_by_reference")
                            .queryParam("tx_ref", payment.getPayerReferenceLast4())
                            .build())
                    .retrieve()
                    .body(FlutterwaveVerifyResponse.class);

            if (response == null || response.data() == null) {
                log.warn("Réponse vide de Flutterwave pour le paiement {}", payment.getId());
                return;
            }

            PaymentStatus newStatus = mapFlutterwaveStatus(response.data().status());

            if (newStatus != null && newStatus != payment.getStatus()) {
                if (newStatus==PaymentStatus.SUCCEEDED){
                    payment.markSucceeded(payment.getGatewayReference());
                }

                log.info("Paiement {} mis à jour: {} -> {}", payment.getId(), PaymentStatus.PENDING, newStatus);
            }

        } catch (Exception e) {
            log.warn("Échec de vérification du paiement {}", payment.getId(), e);
        }
    }

    private boolean isTooOld(Payment payment) {
        return payment.getCreatedAt() != null
                && payment.getCreatedAt().isBefore(Instant.now().minus(MAX_PENDING_AGE));
    }

    private void markExpired(Payment payment) {
        payment.markFailed("Paiement {} expiré après {} sans confirmation");
        paymentRepository.save(payment);
        log.info("Paiement {} expiré après {} sans confirmation", payment.getId(), MAX_PENDING_AGE);
    }

    private PaymentStatus mapFlutterwaveStatus(String flutterwaveStatus) {
        if (flutterwaveStatus == null) {
            return null;
        }
        return switch (flutterwaveStatus.toLowerCase()) {
            case "successful" -> PaymentStatus.SUCCEEDED;
            case "failed" -> PaymentStatus.FAILED;
            default -> null; // "pending" ou autre -> on ne touche pas au statut
        };
    }

    private record FlutterwaveVerifyResponse(String status, String message, FlutterwaveData data) {
    }

    private record FlutterwaveData(long id, String tx_ref, String flw_ref, String status,
                                   String amount, String currency) {
    }
}