package com.guentours.payment.gateway.flutterwave;

import com.flutterwave.bean.Authorization;
import com.flutterwave.bean.Data;
import com.flutterwave.bean.Meta;
import com.flutterwave.bean.Response;
import com.guentours.payment.gateway.AuthorizationChallenge;
import com.guentours.payment.gateway.ChargeStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link FlutterwaveCardGateway#resolveChargeOutcome} against two real captured Flutterwave
 * responses: a charge settled immediately with no authorization step at all (data-only, no meta -
 * common on the sandbox and for many card BINs), and a charge requiring a PIN (meta-only, no data).
 * An earlier version of this parsing required BOTH shapes to be treated as one, unconditionally
 * demanding {@code meta.authorization} - so a fully successful data-only charge was wrongly marked
 * FAILED with "Statut ou structure gateway inattendu : success".
 */
class FlutterwaveCardGatewayTest {

    private final FlutterwaveCardGateway gateway = new FlutterwaveCardGateway(
            new FlutterwaveProperties(null, null, null, null, "XAF", "CM", "https://example.com/payments/redirect"));

    @Test
    void treatsADataOnlySuccessfulResponseAsSucceeded() {
        Response response = new Response();
        response.setStatus("success");
        response.setMessage("Successful");
        Data data = new Data();
        data.setStatus("successful");
        data.setFlw_ref("FLW-MOCK-9546182504e1be3019019904f81c765c");
        response.setData(data);

        var result = gateway.resolveChargeOutcome(response, "tx-ref-1", "7450");

        assertThat(result.status()).isEqualTo(ChargeStatus.SUCCEEDED);
        assertThat(result.gatewayReference()).isEqualTo("tx-ref-1");
    }

    @Test
    void treatsADataOnlyPendingResponseAsPending() {
        Response response = new Response();
        response.setStatus("success");
        Data data = new Data();
        data.setStatus("pending");
        response.setData(data);

        var result = gateway.resolveChargeOutcome(response, "tx-ref-2", null);

        assertThat(result.status()).isEqualTo(ChargeStatus.PENDING);
    }

    @Test
    void treatsAMetaOnlyPinChallengeAsPendingAuthorization() {
        Response response = new Response();
        response.setStatus("success");
        Authorization authorization = new Authorization();
        authorization.setMode("pin");
        Meta meta = new Meta();
        meta.setAuthorization(authorization);
        response.setMeta(meta);

        var result = gateway.resolveChargeOutcome(response, "tx-ref-3", "7450");

        assertThat(result.status()).isEqualTo(ChargeStatus.PENDING_AUTHORIZATION);
        assertThat(result.authorizationChallenge().type()).isEqualTo(AuthorizationChallenge.AuthorizationType.PIN);
    }

    @Test
    void treatsAMetaOnlyOtpChallengeAsPendingAuthorization() {
        // What a PIN submission's "Charge authorization data required" response looks like when
        // Flutterwave chains a second factor: still no data, meta.authorization now asks for OTP.
        Response response = new Response();
        response.setStatus("success");
        response.setMessage("Charge authorization data required");
        Authorization authorization = new Authorization();
        authorization.setMode("otp");
        Meta meta = new Meta();
        meta.setAuthorization(authorization);
        response.setMeta(meta);

        var result = gateway.resolveChargeOutcome(response, "tx-ref-4", null);

        assertThat(result.status()).isEqualTo(ChargeStatus.PENDING_AUTHORIZATION);
        assertThat(result.authorizationChallenge().type()).isEqualTo(AuthorizationChallenge.AuthorizationType.OTP);
    }

    @Test
    void prefersTheRedirectChallengeOverDataPendingWhenBothArePresent() {
        // Real captured trace: some cards return data.status="pending" AND meta.authorization
        // (mode=redirect) on the very same response - data is just a placeholder here, the challenge
        // is what actually needs handling, so checking data first would silently swallow it as an
        // ordinary async wait and neither prompt the payer nor send them to the 3DS page.
        Response response = new Response();
        response.setStatus("success");
        response.setMessage("Charge initiated");
        Data data = new Data();
        data.setStatus("pending");
        response.setData(data);
        Authorization authorization = new Authorization();
        authorization.setMode("redirect");
        authorization.setRedirect("https://ravesandboxapi.flutterwave.com/mockvbvpage?ref=FLW-MOCK-x");
        Meta meta = new Meta();
        meta.setAuthorization(authorization);
        response.setMeta(meta);

        var result = gateway.resolveChargeOutcome(response, "tx-ref-7", "4242");

        assertThat(result.status()).isEqualTo(ChargeStatus.PENDING_AUTHORIZATION);
        assertThat(result.authorizationChallenge().type()).isEqualTo(AuthorizationChallenge.AuthorizationType.REDIRECT);
        assertThat(result.authorizationChallenge().redirectUrl())
                .isEqualTo("https://ravesandboxapi.flutterwave.com/mockvbvpage?ref=FLW-MOCK-x");
    }

    @Test
    void treatsAMetaOnlyRedirectChallengeAsPendingAuthorizationWithUrl() {
        Response response = new Response();
        response.setStatus("success");
        Authorization authorization = new Authorization();
        authorization.setMode("redirect");
        authorization.setRedirect("https://bank.example/3ds");
        Meta meta = new Meta();
        meta.setAuthorization(authorization);
        response.setMeta(meta);

        var result = gateway.resolveChargeOutcome(response, "tx-ref-5", null);

        assertThat(result.status()).isEqualTo(ChargeStatus.PENDING_AUTHORIZATION);
        assertThat(result.authorizationChallenge().redirectUrl()).isEqualTo("https://bank.example/3ds");
    }

    @Test
    void failsWhenNeitherDataNorAuthorizationMetaArePresent() {
        Response response = new Response();
        response.setStatus("success");

        var result = gateway.resolveChargeOutcome(response, "tx-ref-6", null);

        assertThat(result.status()).isEqualTo(ChargeStatus.FAILED);
        assertThat(result.failureReason()).contains("Statut ou structure gateway inattendu");
    }
}
