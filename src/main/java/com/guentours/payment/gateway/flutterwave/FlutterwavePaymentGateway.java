package com.guentours.payment.gateway.flutterwave;

import com.guentours.payment.gateway.ChargeRequest;
import com.guentours.payment.gateway.ChargeResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * CARD is kept here as a fallback route (an admin can still point a country/method at Flutterwave),
 * MOBILE_MONEY is the one method Stripe genuinely can't do so this stays its real home. GOOGLE_PAY/
 * APPLE_PAY are no longer handled here - see StripePaymentGateway - Flutterwave's own attempt at
 * them (FlutterwaveApplePayGateway) turned out to call the Google Pay SDK by copy-paste mistake and
 * never had a way to hand the resulting hosted-checkout redirect back to the frontend anyway.
 */
@Component("FLUTTERWAVE")
@Profile("!test")
public class FlutterwavePaymentGateway implements com.guentours.payment.gateway.PaymentGateway {

    private final FlutterwaveCardGateway cardGateway;
    private final FlutterwaveMobileMoneyGateway mobileMoneyGateway;
    private final FlutterwavePaypalGateway paypalGateway;

    FlutterwavePaymentGateway(FlutterwaveCardGateway cardGateway, FlutterwaveMobileMoneyGateway mobileMoneyGateway,
                              FlutterwavePaypalGateway paypalGateway) {
        this.cardGateway = cardGateway;
        this.mobileMoneyGateway = mobileMoneyGateway;
        this.paypalGateway = paypalGateway;
    }

    @Override
    public ChargeResult charge(ChargeRequest request) {
        return switch (request.paymentMethod()) {
            case CARD -> cardGateway.initiate(request, request.paymentReference());
            case MOBILE_MONEY -> mobileMoneyGateway.charge(request, request.paymentReference());
            case PAYPAL -> paypalGateway.charge(request, request.paymentReference());
            case GOOGLE_PAY, APPLE_PAY -> ChargeResult.declined(
                    request.paymentMethod() + " n'est plus géré par Flutterwave - route-le vers STRIPE.");
        };
    }

    @Override
    public ChargeResult completeCardPinAuthorization(String paymentId, ChargeRequest originalRequest, String pin) {
        return cardGateway.completeWithPin(paymentId, originalRequest.cardNumber(), originalRequest.cvv(),
                originalRequest.expiry(), originalRequest.currency(), originalRequest.amount(),
                originalRequest.cardHolderName(), originalRequest.customerEmail(), pin);
    }
}