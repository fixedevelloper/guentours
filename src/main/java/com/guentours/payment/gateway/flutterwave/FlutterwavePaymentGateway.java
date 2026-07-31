package com.guentours.payment.gateway.flutterwave;

import com.guentours.payment.domain.PaymentMethod;
import com.guentours.payment.gateway.ChargeRequest;
import com.guentours.payment.gateway.ChargeResult;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("FLUTTERWAVE")
@Profile("!test")
@Primary
public class FlutterwavePaymentGateway implements com.guentours.payment.gateway.PaymentGateway {

    private final FlutterwaveCardGateway cardGateway;
    private final FlutterwaveMobileMoneyGateway mobileMoneyGateway;
    private final FlutterwaveGooglePayGateway googlePayGateway;
    private final FlutterwaveApplePayGateway applePayGateway;
    private final FlutterwavePaypalGateway paypalGateway;

    FlutterwavePaymentGateway(FlutterwaveCardGateway cardGateway, FlutterwaveMobileMoneyGateway mobileMoneyGateway,
                              FlutterwaveGooglePayGateway googlePayGateway, FlutterwaveApplePayGateway applePayGateway,
                              FlutterwavePaypalGateway paypalGateway) {
        this.cardGateway = cardGateway;
        this.mobileMoneyGateway = mobileMoneyGateway;
        this.googlePayGateway = googlePayGateway;
        this.applePayGateway = applePayGateway;
        this.paypalGateway = paypalGateway;
    }

    @Override
    public ChargeResult charge(ChargeRequest request) {
        return switch (request.paymentMethod()) {
            case CARD -> cardGateway.initiate(request, request.paymentReference());
            case MOBILE_MONEY -> mobileMoneyGateway.charge(request, request.paymentReference());
            case GOOGLE_PAY -> googlePayGateway.charge(request, request.paymentReference());
            case APPLE_PAY -> applePayGateway.charge(request, request.paymentReference());
            case PAYPAL -> paypalGateway.charge(request, request.paymentReference());
        };
    }

    @Override
    public ChargeResult completeCardPinAuthorization(String paymentId, ChargeRequest originalRequest, String pin) {
        return cardGateway.completeWithPin(paymentId, originalRequest.cardNumber(), originalRequest.cvv(),
                originalRequest.expiry(), originalRequest.currency(), originalRequest.amount(),
                originalRequest.cardHolderName(), originalRequest.customerEmail(), pin);
    }
}