package com.guentours.payment.gateway.flutterwave;

import com.flutterwave.utility.Environment;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FlutterwaveInitializer {

    private final FlutterwaveProperties properties;

    public FlutterwaveInitializer(FlutterwaveProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        requireNonBlank(properties.secretKey(), "flutterwave.secret-key");
        requireNonBlank(properties.publicKey(), "flutterwave.public-key");
        requireNonBlank(properties.encryptionKey(), "flutterwave.encryption-key");

        Environment.setSecretKey(properties.secretKey());
        Environment.setPublicKey(properties.publicKey());
        Environment.setEncryptionKey(properties.encryptionKey());

        log.info("Flutterwave initialisé avec la clé secrète {}...{} (mode {})",
                mask(properties.secretKey()), properties.secretKey().startsWith("FLWSECK_TEST") ? "TEST" : "LIVE");
    }

    private void requireNonBlank(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Propriété manquante ou vide : " + propertyName + " — vérifie les variables d'environnement.");
        }
    }

    private String mask(String key) {
        return key.length() > 10 ? key.substring(0, 6) + "***" + key.substring(key.length() - 4) : "***";
    }
}