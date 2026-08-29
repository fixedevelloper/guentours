package com.guentours.payment.gateway;

import com.guentours.payment.domain.PaymentMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

/**
 * Stands in for the real Stripe/Flutterwave gateways in the test profile, so integration tests
 * never hit a live provider. Registered under both bean names ("STRIPE" - the default provider -
 * and "FLUTTERWAVE" - still routed to for MOBILE_MONEY) by {@link PaymentGatewayTestConfig}, so
 * PaymentProviderRoutingService's provider resolution works the same way in every profile.
 */
public class MockPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGateway.class);

    @Override
    public ChargeResult charge(ChargeRequest request) {
        log.info("Charging {} via {} ending {}",
                request.amount(),
                request.paymentMethod(),
                request.payerReferenceLast4());

        if (request.paymentMethod() == PaymentMethod.CARD) {
            String cardNumber = Objects.toString(request.cardNumber(), "");
            String cvv = Objects.toString(request.cvv(), "");

            if (cardNumber.endsWith("0000") || "000".equals(cvv)) {
                return ChargeResult.declined("Card declined by issuer");
            }
            if (cardNumber.endsWith("1111")) {
                return ChargeResult.pendingAuthorization("MOCK-CARD-" + generateRef(),
                        new AuthorizationChallenge(AuthorizationChallenge.AuthorizationType.PIN, null));
            }
            return ChargeResult.success("MOCK-CARD-" + generateRef());
        }

        // Gestion Mobile Money (MTN / ORANGE)
        String mobileNumber = Objects.toString(request.mobileNumber(), "");

        if (mobileNumber.endsWith("0000")) {
            return ChargeResult.declined("Mobile money payment declined - insufficient funds or invalid PIN");
        }

        String prefix = request.paymentMethod() == PaymentMethod.MOBILE_MONEY ? "MTN" : "ORANGE";
        return ChargeResult.success(prefix + "-" + generateRef());
    }

    /** Test convention mirroring the real PIN dance: "1234" succeeds, anything else is declined. */
    @Override
    public ChargeResult completeCardPinAuthorization(String paymentId, ChargeRequest originalRequest, String pin) {
        if ("1234".equals(pin)) {
            return ChargeResult.success("MOCK-CARD-" + generateRef());
        }
        return ChargeResult.declined("Incorrect PIN");
    }

    private String generateRef() {
        return UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }
}