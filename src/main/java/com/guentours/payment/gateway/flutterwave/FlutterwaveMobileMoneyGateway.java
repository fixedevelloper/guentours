package com.guentours.payment.gateway.flutterwave;

import com.flutterwave.bean.*;
import com.flutterwave.services.MobileMoney;
import com.guentours.payment.gateway.ChargeRequest;
import com.guentours.payment.gateway.ChargeResult;
import com.guentours.payment.gateway.ChargeStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class FlutterwaveMobileMoneyGateway {

    private static final String STATUS_ERROR = "error";
    private static final String STATUS_SUCCESS = "success";

    ChargeResult charge(ChargeRequest request, String paymentReference) {
        MobileMoneyRegion region;
        try {
            region = MobileMoneyRegion.resolve(request.countryCode());
        } catch (UnsupportedMobileMoneyCountryException e) {
            log.warn("Mobile money non supporté pour tx_ref {} : {}", paymentReference, e.getMessage());
            return new ChargeResult(ChargeStatus.FAILED, paymentReference,
                    request.payerReferenceLast4(), e.getMessage(), null);
        }

        Response response;
        try {
            response = switch (region) {
                case FRANCOPHONE -> new MobileMoney().runFrancophoneMobileMoneyTransaction(
                        new FrancophoneMobileMoneyRequestRequest(
                                paymentReference, request.amount(), request.countryCode(),
                                request.countryCurrency(), request.customerEmail(),
                                request.mobileNumber(), request.mobileNumber()));

                case RWANDA -> new MobileMoney().runRwandaMobileMoneyTransaction(
                        new RwandaMobileMoneyRequestRequest(
                                paymentReference, request.amount(), request.countryCurrency(),
                                request.customerEmail(), request.mobileNumber(), request.mobileNumber()));

                case MPESA -> new MobileMoney().runMpesaTransaction(
                        new MpesaRequest(
                                paymentReference, request.amount(), request.countryCurrency(),
                                request.customerEmail(), request.mobileNumber(), request.mobileNumber()));

                case GHANA -> new MobileMoney().runGhanaMobileMoneyTransaction(
                        new GhanaMobileMoneyRequestRequest(
                                paymentReference, request.amount(), request.countryCurrency(),
                                MobileNetworkInferrer.infer(region, request.mobileNumber()),
                                MobileNetworkInferrer.infer(region, request.mobileNumber()),
                                request.customerEmail(), request.mobileNumber(), request.mobileNumber(),
                                request.customerIp(), null, null));

                case UGANDA -> new MobileMoney().runUgandaMobileMoneyTransaction(
                        new UgandaMobileMoneyRequestRequest(
                                paymentReference, request.amount(), request.countryCurrency(),
                                MobileNetworkInferrer.infer(region, request.mobileNumber()),
                                MobileNetworkInferrer.infer(region, request.mobileNumber()),
                                request.customerEmail(), request.mobileNumber(), request.mobileNumber(),
                                request.customerIp(), null, null));

                case ZAMBIA -> new MobileMoney().runZambiaMobileMoneyTransaction(
                        new ZambiaMobileMoneyRequestRequest(
                                paymentReference, request.amount(), request.countryCurrency(),
                                MobileNetworkInferrer.infer(region, request.mobileNumber()),
                                MobileNetworkInferrer.infer(region, request.mobileNumber()),
                                request.customerEmail(), request.mobileNumber(), request.mobileNumber(),
                                request.customerIp(), null, null));
            };
        } catch (Exception e) {
            log.error("Erreur Flutterwave mobile money ({}) pour tx_ref {}", region, paymentReference, e);
            return new ChargeResult(ChargeStatus.FAILED, paymentReference,
                    request.payerReferenceLast4(), "Erreur de communication avec le gateway", null);
        }

        if (response == null) {
            return new ChargeResult(ChargeStatus.FAILED, paymentReference,
                    request.payerReferenceLast4(), "Réponse gateway vide", null);
        }

        log.info("Réponse Flutterwave mobile money ({}) tx_ref={} status={} message={}",
                region, paymentReference, response.getStatus(), response.getMessage());

        if (STATUS_ERROR.equalsIgnoreCase(response.getStatus())) {
            return new ChargeResult(ChargeStatus.FAILED, paymentReference,
                    request.payerReferenceLast4(), response.getMessage(), null);
        }
        if (!STATUS_SUCCESS.equalsIgnoreCase(response.getStatus())) {
            return new ChargeResult(ChargeStatus.FAILED, paymentReference,
                    request.payerReferenceLast4(), "Statut gateway inattendu : " + response.getStatus(), null);
        }

        return new ChargeResult(ChargeStatus.PENDING, paymentReference, request.payerReferenceLast4(), null, null);
    }
}