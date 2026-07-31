package com.guentours.payment.service;

import com.guentours.payment.domain.PaymentMethod;
import com.guentours.payment.domain.PaymentProviderRoute;
import com.guentours.payment.domain.PaymentProviderRouteRepository;
import com.guentours.payment.gateway.PaymentGateway;
import com.guentours.shared.exception.BusinessException;
import com.guentours.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves which {@link PaymentGateway} handles a charge for a given country + payment method, and
 * lets admins manage that routing (e.g. Flutterwave for CARD in Cameroon, a different operator for
 * MOBILE_MONEY) - see {@link PaymentProviderRoute}. {@code gatewaysByProvider} is populated by
 * Spring from every {@link PaymentGateway} bean, keyed by its bean name ("FLUTTERWAVE", "GENERIC",
 * ...), so adding a real second provider later is just deploying a new named bean and pointing a
 * route at it - no change needed here.
 */
@Service
public class PaymentProviderRoutingService {

    /** Used whenever no admin-configured route matches, so behavior is unchanged until one is added. */
    public static final String DEFAULT_PROVIDER = "FLUTTERWAVE";

    private final PaymentProviderRouteRepository routeRepository;
    private final Map<String, PaymentGateway> gatewaysByProvider;

    public PaymentProviderRoutingService(PaymentProviderRouteRepository routeRepository,
                                         Map<String, PaymentGateway> gatewaysByProvider) {
        this.routeRepository = routeRepository;
        this.gatewaysByProvider = gatewaysByProvider;
    }

    /** Provider names with an actual deployed gateway - what the admin dashboard may route to. */
    public Set<String> availableProviderNames() {
        return gatewaysByProvider.keySet();
    }

    public PaymentGateway resolveGateway(String countryCode, PaymentMethod paymentMethod) {
        String providerName = resolveProviderName(countryCode, paymentMethod);
        PaymentGateway gateway = gatewaysByProvider.get(providerName);
        if (gateway == null) {
            throw new BusinessException(
                    "Fournisseur de paiement configuré mais non déployé : " + providerName);
        }
        return gateway;
    }

    /**
     * The country-specific rule wins when one exists for this exact country + method, even if the
     * only other rule is the global default; falls back to the global (country-less) rule, then to
     * {@link #DEFAULT_PROVIDER} when nothing has been configured at all. A rule that matches but is
     * deactivated rejects the charge outright rather than silently falling through to a broader rule -
     * that's the whole point of letting an admin disable one.
     */
    String resolveProviderName(String countryCode, PaymentMethod paymentMethod) {
        if (countryCode != null) {
            var specific = routeRepository.findByCountryCodeAndPaymentMethod(countryCode, paymentMethod);
            if (specific.isPresent()) {
                return requireActive(specific.get(), countryCode);
            }
        }

        var global = routeRepository.findByCountryCodeIsNullAndPaymentMethod(paymentMethod);
        if (global.isPresent()) {
            return requireActive(global.get(), null);
        }

        return DEFAULT_PROVIDER;
    }

    private String requireActive(PaymentProviderRoute route, String countryCode) {
        if (!route.isActive()) {
            throw new BusinessException(countryCode != null
                    ? "Le paiement par " + route.getPaymentMethod() + " est actuellement désactivé pour " + countryCode + "."
                    : "Le paiement par " + route.getPaymentMethod() + " est actuellement désactivé.");
        }
        return route.getProviderName();
    }

    public List<PaymentProviderRoute> findAll() {
        return routeRepository.findAllByOrderByPaymentMethodAscCountryCodeAsc();
    }

    @Transactional
    public PaymentProviderRoute create(String countryCode, PaymentMethod paymentMethod, String providerName) {
        validateProviderName(providerName);
        if (findExisting(countryCode, paymentMethod).isPresent()) {
            throw new BusinessException("Une règle existe déjà pour " + paymentMethod
                    + (countryCode != null ? " en " + countryCode : " (règle globale)") + ".");
        }
        return routeRepository.save(new PaymentProviderRoute(countryCode, paymentMethod, providerName));
    }

    @Transactional
    public PaymentProviderRoute update(String id, String providerName, Boolean active) {
        PaymentProviderRoute route = routeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Règle de routage introuvable : " + id));
        if (providerName != null) {
            validateProviderName(providerName);
            route.setProviderName(providerName);
        }
        if (active != null) {
            route.setActive(active);
        }
        return route;
    }

    private java.util.Optional<PaymentProviderRoute> findExisting(String countryCode, PaymentMethod paymentMethod) {
        return countryCode != null
                ? routeRepository.findByCountryCodeAndPaymentMethod(countryCode, paymentMethod)
                : routeRepository.findByCountryCodeIsNullAndPaymentMethod(paymentMethod);
    }

    private void validateProviderName(String providerName) {
        if (!gatewaysByProvider.containsKey(providerName)) {
            throw new BusinessException("Fournisseur de paiement inconnu : " + providerName
                    + ". Fournisseurs disponibles : " + gatewaysByProvider.keySet());
        }
    }
}
