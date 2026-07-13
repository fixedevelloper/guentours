package com.guentours.payment.gateway;

import com.guentours.payment.PaymentMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stand-in for a real processor (Stripe/Adyen for cards, MTN/Orange's own mobile money APIs)
 * so the checkout flow is fully exercisable without live payment processing. Swap this out for
 * a real {@link PaymentGateway} implementation behind a config property once merchant
 * credentials are available - {@link com.guentours.payment.PaymentService} only depends on
 * this interface, never on gateway internals.
 *
 * Test cards: a PAN ending in "0000" or a CVV of "000" simulate a decline. For mobile money, a
 * mobile number ending in "0000" simulates a decline.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGateway.class);

    @Override
    public ChargeResult charge(ChargeRequest request) {
        log.info("Charging {} via {} ending {}", request.amount(), request.paymentMethod(), request.payerReferenceLast4());

        if (request.paymentMethod() == PaymentMethod.CARD) {
            if (request.cardNumber().endsWith("0000") || "000".equals(request.cvv())) {
                return ChargeResult.declined("Card declined by issuer");
            }
            return ChargeResult.success("MOCK-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        }

        if (request.mobileNumber().endsWith("0000")) {
            return ChargeResult.declined("Mobile money payment declined - insufficient funds or invalid PIN");
        }
        String prefix = request.paymentMethod() == PaymentMethod.MTN_MOBILE_MONEY ? "MTN" : "ORANGE";
        return ChargeResult.success(prefix + "-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
    }
}
