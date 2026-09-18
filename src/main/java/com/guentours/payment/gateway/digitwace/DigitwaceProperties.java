package com.guentours.payment.gateway.digitwace;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WacePay PayIn (Digitwace) config - see https://docs.digitwace.com/docs/wacepay-payin/get-started.
 * {@code tokenPath}/{@code initPaymentPath}/{@code statusPath} are kept overridable rather than
 * hardcoded because, at integration time, the docs site's API reference pages (get-token,
 * init-payment, get-status) render their request/response schema client-side and didn't expose the
 * exact HTTP paths/fields to a static fetch. The defaults below are best-effort guesses from the
 * docs site's page structure (e.g. wallet/init-payment -> {@code POST /wallet/create}) - confirm
 * them against Digitwace's Postman collection/OpenAPI spec and correct here (or via env vars)
 * without a code change.
 */
@ConfigurationProperties(prefix = "app.digitwace")
public record DigitwaceProperties(
        String baseUrl,
        String publicKey,
        String privateKey,
        String tokenPath,
        long tokenTtlSeconds,
        String initPaymentPath,
        String statusPath,
        String defaultCurrency,
        String defaultCountry
) {
    public String baseUrlOrDefault() {
        return blank(baseUrl) ? "https://payinws.wacepay.com/api/v1" : baseUrl;
    }

    public String tokenPathOrDefault() {
        return blank(tokenPath) ? "/auth/token" : tokenPath;
    }

    public String initPaymentPathOrDefault() {
        return blank(initPaymentPath) ? "/wallet/create" : initPaymentPath;
    }

    /** Expected to contain a {@code {transactionId}} placeholder. */
    public String statusPathOrDefault() {
        return blank(statusPath) ? "/wallet/status/{transactionId}" : statusPath;
    }

    /** Conservative guess pending the real token lifetime - refreshed 30s before this elapses. */
    public long tokenTtlSecondsOrDefault() {
        return tokenTtlSeconds > 0 ? tokenTtlSeconds : 3300;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
