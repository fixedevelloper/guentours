package com.guentours.payment.gateway;

import com.guentours.payment.domain.PaymentMethod;
import com.guentours.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
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

    private String generateRef() {
        return UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }
}