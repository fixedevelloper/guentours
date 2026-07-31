package com.guentours.payment.web;

import com.guentours.payment.service.PaymentProviderRoutingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * Admin-only management of which payment gateway handles a given payment method per country (see
 * {@code /api/admin/**} in SecurityConfig). Lets an admin, for instance, route CARD payments in
 * Cameroon to Flutterwave while routing MOBILE_MONEY there to a different operator, and flip that
 * at any time without a deploy.
 */
@RestController
@RequestMapping("/api/admin/payment-provider-routes")
public class PaymentProviderRouteController {

    private final PaymentProviderRoutingService routingService;

    public PaymentProviderRouteController(PaymentProviderRoutingService routingService) {
        this.routingService = routingService;
    }

    @GetMapping
    public ResponseEntity<List<PaymentProviderRouteResponse>> list() {
        var response = routingService.findAll().stream().map(PaymentProviderRouteResponse::of).toList();
        return ResponseEntity.ok(response);
    }

    /** Provider names with an actually deployed gateway - what the create/edit form may pick from. */
    @GetMapping("/available-providers")
    public ResponseEntity<Set<String>> availableProviders() {
        return ResponseEntity.ok(routingService.availableProviderNames());
    }

    @PostMapping
    public ResponseEntity<PaymentProviderRouteResponse> create(@RequestBody PaymentProviderRouteRequest request) {
        var route = routingService.create(request.countryCode(), request.paymentMethod(), request.providerName());
        return ResponseEntity.ok(PaymentProviderRouteResponse.of(route));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentProviderRouteResponse> update(@PathVariable String id,
                                                               @RequestBody PaymentProviderRouteRequest request) {
        var route = routingService.update(id, request.providerName(), request.active());
        return ResponseEntity.ok(PaymentProviderRouteResponse.of(route));
    }
}
