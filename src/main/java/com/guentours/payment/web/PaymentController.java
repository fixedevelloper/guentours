package com.guentours.payment.web;

import com.guentours.payment.domain.Payment;
import com.guentours.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Payment> pay(@Valid @RequestBody PaymentRequest request,
                                       HttpServletRequest httpRequest) {
        PaymentRequest requestWithIp = request.withCustomerIp(resolveClientIp(httpRequest));
        Payment payment = paymentService.pay(requestWithIp);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getById(@PathVariable String paymentId) {
        return ResponseEntity.ok(paymentService.getById(paymentId));
    }

    /**
     * Extrait l'IP réelle du client. Si l'app est derrière un reverse proxy/load balancer
     * (Nginx, ALB, Cloudflare...), X-Forwarded-For contient la vraie IP du client en première
     * position ; sinon on retombe sur l'IP de connexion directe.
     *
     * ⚠️ X-Forwarded-For est falsifiable par le client si le proxy ne le réécrit pas correctement.
     * Vérifie que ton reverse proxy écrase bien cet en-tête plutôt que de l'ajouter en aveugle,
     * sinon un client peut usurper une fausse IP ici.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}