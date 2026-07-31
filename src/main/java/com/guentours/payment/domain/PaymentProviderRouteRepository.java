package com.guentours.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentProviderRouteRepository extends JpaRepository<PaymentProviderRoute, String> {

    Optional<PaymentProviderRoute> findByCountryCodeAndPaymentMethod(String countryCode, PaymentMethod paymentMethod);

    Optional<PaymentProviderRoute> findByCountryCodeIsNullAndPaymentMethod(PaymentMethod paymentMethod);

    List<PaymentProviderRoute> findAllByOrderByPaymentMethodAscCountryCodeAsc();
}
