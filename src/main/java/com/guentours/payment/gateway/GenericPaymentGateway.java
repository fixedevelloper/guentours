package com.guentours.payment.gateway;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Placeholder second provider so {@code PaymentProviderRoutingService} can actually route between
 * more than one gateway end to end before a real second payment operator is integrated. Selecting
 * it from the admin dashboard is a deliberate no-op today: it fails every charge with a clear
 * reason rather than silently pretending to process a real payment.
 */
@Component("GENERIC")
@Profile("!test")
class GenericPaymentGateway implements PaymentGateway {

    @Override
    public ChargeResult charge(ChargeRequest request) {
        return ChargeResult.declined(
                "Le fournisseur de paiement GENERIC n'est pas encore implémenté - aucune charge réelle n'a été tentée.");
    }
}
