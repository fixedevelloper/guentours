package com.guentours.payment.web;

import com.guentours.payment.domain.PaymentMethod;

/**
 * Create/update payload. {@code countryCode} null on create means a global default rule for that
 * {@code paymentMethod}. On update, {@code providerName}/{@code active} left null leave that
 * attribute unchanged (country/method are immutable after creation - delete and recreate instead).
 */
public record PaymentProviderRouteRequest(String countryCode, PaymentMethod paymentMethod,
                                          String providerName, Boolean active) {
}
