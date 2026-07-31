package com.guentours.payment.web;

import com.guentours.payment.domain.PaymentMethod;
import com.guentours.payment.domain.PaymentProviderRoute;

public record PaymentProviderRouteResponse(String id, String countryCode, PaymentMethod paymentMethod,
                                           String providerName, boolean active) {

    static PaymentProviderRouteResponse of(PaymentProviderRoute route) {
        return new PaymentProviderRouteResponse(route.getId(), route.getCountryCode(),
                route.getPaymentMethod(), route.getProviderName(), route.isActive());
    }
}
