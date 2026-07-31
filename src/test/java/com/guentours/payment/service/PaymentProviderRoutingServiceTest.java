package com.guentours.payment.service;

import com.guentours.payment.domain.PaymentMethod;
import com.guentours.payment.domain.PaymentProviderRoute;
import com.guentours.payment.domain.PaymentProviderRouteRepository;
import com.guentours.payment.gateway.ChargeRequest;
import com.guentours.payment.gateway.ChargeResult;
import com.guentours.payment.gateway.PaymentGateway;
import com.guentours.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProviderRoutingServiceTest {

    @Mock
    private PaymentProviderRouteRepository routeRepository;
    @Mock
    private PaymentGateway flutterwaveGateway;
    @Mock
    private PaymentGateway genericGateway;

    private PaymentProviderRoutingService routingService;

    @BeforeEach
    void setUp() {
        routingService = new PaymentProviderRoutingService(routeRepository,
                Map.of("FLUTTERWAVE", flutterwaveGateway, "GENERIC", genericGateway));
    }

    @Test
    void fallsBackToFlutterwaveWhenNoRouteIsConfigured() {
        when(routeRepository.findByCountryCodeAndPaymentMethod("CM", PaymentMethod.CARD)).thenReturn(Optional.empty());
        when(routeRepository.findByCountryCodeIsNullAndPaymentMethod(PaymentMethod.CARD)).thenReturn(Optional.empty());

        assertThat(routingService.resolveGateway("CM", PaymentMethod.CARD)).isSameAs(flutterwaveGateway);
    }

    @Test
    void prefersTheCountrySpecificRouteOverTheGlobalOne() {
        var countryRoute = new PaymentProviderRoute("CM", PaymentMethod.MOBILE_MONEY, "GENERIC");
        when(routeRepository.findByCountryCodeAndPaymentMethod("CM", PaymentMethod.MOBILE_MONEY))
                .thenReturn(Optional.of(countryRoute));

        assertThat(routingService.resolveGateway("CM", PaymentMethod.MOBILE_MONEY)).isSameAs(genericGateway);
    }

    @Test
    void fallsBackToTheGlobalRouteWhenNoCountrySpecificOneMatches() {
        when(routeRepository.findByCountryCodeAndPaymentMethod("SN", PaymentMethod.CARD)).thenReturn(Optional.empty());
        var globalRoute = new PaymentProviderRoute(null, PaymentMethod.CARD, "GENERIC");
        when(routeRepository.findByCountryCodeIsNullAndPaymentMethod(PaymentMethod.CARD))
                .thenReturn(Optional.of(globalRoute));

        assertThat(routingService.resolveGateway("SN", PaymentMethod.CARD)).isSameAs(genericGateway);
    }

    @Test
    void rejectsWhenTheMostSpecificMatchingRouteIsDisabled() {
        var countryRoute = new PaymentProviderRoute("CM", PaymentMethod.CARD, "FLUTTERWAVE");
        countryRoute.setActive(false);
        when(routeRepository.findByCountryCodeAndPaymentMethod("CM", PaymentMethod.CARD))
                .thenReturn(Optional.of(countryRoute));

        assertThatThrownBy(() -> routingService.resolveGateway("CM", PaymentMethod.CARD))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("désactivé");
    }

    @Test
    void rejectsWhenTheGlobalRouteIsDisabledAndNoCountrySpecificOneExists() {
        when(routeRepository.findByCountryCodeAndPaymentMethod("SN", PaymentMethod.PAYPAL)).thenReturn(Optional.empty());
        var globalRoute = new PaymentProviderRoute(null, PaymentMethod.PAYPAL, "FLUTTERWAVE");
        globalRoute.setActive(false);
        when(routeRepository.findByCountryCodeIsNullAndPaymentMethod(PaymentMethod.PAYPAL))
                .thenReturn(Optional.of(globalRoute));

        assertThatThrownBy(() -> routingService.resolveGateway("SN", PaymentMethod.PAYPAL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("désactivé");
    }

    @Test
    void rejectsAConfiguredProviderThatHasNoDeployedGateway() {
        var route = new PaymentProviderRoute("CM", PaymentMethod.CARD, "SOME_UNDEPLOYED_PROVIDER");
        when(routeRepository.findByCountryCodeAndPaymentMethod("CM", PaymentMethod.CARD)).thenReturn(Optional.of(route));

        assertThatThrownBy(() -> routingService.resolveGateway("CM", PaymentMethod.CARD))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("non déployé");
    }

    @Test
    void availableProviderNamesReflectsTheDeployedGateways() {
        assertThat(routingService.availableProviderNames()).containsExactlyInAnyOrder("FLUTTERWAVE", "GENERIC");
    }

    @Test
    void createRejectsADuplicateRuleForTheSameCountryAndMethod() {
        when(routeRepository.findByCountryCodeAndPaymentMethod("CM", PaymentMethod.CARD))
                .thenReturn(Optional.of(new PaymentProviderRoute("CM", PaymentMethod.CARD, "FLUTTERWAVE")));

        assertThatThrownBy(() -> routingService.create("CM", PaymentMethod.CARD, "GENERIC"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("existe déjà");
    }

    @Test
    void createRejectsAnUnknownProviderName() {
        assertThatThrownBy(() -> routingService.create("CM", PaymentMethod.CARD, "NOT_A_REAL_PROVIDER"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inconnu");
    }

    @Test
    void createSucceedsForANewGlobalRule() {
        when(routeRepository.findByCountryCodeIsNullAndPaymentMethod(PaymentMethod.MOBILE_MONEY))
                .thenReturn(Optional.empty());
        when(routeRepository.save(org.mockito.ArgumentMatchers.any(PaymentProviderRoute.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var route = routingService.create(null, PaymentMethod.MOBILE_MONEY, "GENERIC");

        assertThat(route.getCountryCode()).isNull();
        assertThat(route.getProviderName()).isEqualTo("GENERIC");
    }

    // Sanity check that the gateway registry is actually usable for a real charge, not just resolved.
    @Test
    void resolvedGatewayCanActuallyBeInvoked() {
        when(routeRepository.findByCountryCodeAndPaymentMethod("CM", PaymentMethod.CARD)).thenReturn(Optional.empty());
        when(routeRepository.findByCountryCodeIsNullAndPaymentMethod(PaymentMethod.CARD)).thenReturn(Optional.empty());
        when(flutterwaveGateway.charge(org.mockito.ArgumentMatchers.any(ChargeRequest.class)))
                .thenReturn(ChargeResult.success("ref-1"));

        var gateway = routingService.resolveGateway("CM", PaymentMethod.CARD);
        var result = gateway.charge(new ChargeRequest(null, "XAF", "CM", "XAF", PaymentMethod.CARD,
                null, null, null, null, null, null, "ref-1", null, null));

        assertThat(result.isSucceeded()).isTrue();
    }
}
