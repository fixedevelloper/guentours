package com.guentours.payment.domain;

import com.guentours.shared.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "booking_id", nullable = false)
    private String bookingId;

    @Embedded
    private Money amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    /** Last 4 digits only of the card/mobile number - the full PAN/CVV/number are handed to the gateway and never stored. */
    @Column(name = "payer_reference_last4")
    private String payerReferenceLast4;

    /**
     * true si ce paiement correspond à un acompte (PAY_LATER), false si c'est un paiement complet.
     * Mémorisé ici pour que la confirmation asynchrone (webhook) sache, sans avoir à recalculer
     * l'état du booking, quelle transition et quel event appliquer une fois le paiement confirmé.
     */
    @Column(name = "deposit_payment", nullable = false)
    private boolean depositPayment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "gateway_reference", unique = true)
    private String gatewayReference;

    /** Set only while {@link #status} is {@code PENDING_AUTHORIZATION} - which challenge is outstanding. */
    @Enumerated(EnumType.STRING)
    @Column(name = "authorization_type")
    private PaymentAuthorizationType authorizationType;

    /** Set only when {@link #authorizationType} is {@code REDIRECT} - where the client must send the payer. */
    @Column(name = "authorization_redirect_url")
    private String authorizationRedirectUrl;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Payment() {
        // JPA
    }

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "country_currency", nullable = false, length = 3)
    private String countryCurrency;

    public Payment(String bookingId, Money amount, PaymentMethod paymentMethod, String payerReferenceLast4,
                   boolean depositPayment, String countryCode, String countryCurrency) {
        this.bookingId = bookingId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.payerReferenceLast4 = payerReferenceLast4;
        this.depositPayment = depositPayment;
        this.countryCode = countryCode;
        this.countryCurrency = countryCurrency;
        this.status = PaymentStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getCountryCurrency() {
        return countryCurrency;
    }

    public String getCountryCode() {
        return countryCode;
    }

    /**
     * Passe le paiement en attente de confirmation gateway (mobile money en attente de validation USSD,
     * carte en attente de PIN/OTP/redirection 3DS...). Enregistre la référence gateway dès qu'elle est
     * connue pour permettre la corrélation lors du webhook, même si le statut final n'est pas encore acquis.
     */
    public void markPending(String gatewayReference) {
        if (this.status != PaymentStatus.PENDING && this.status != PaymentStatus.PENDING_AUTHORIZATION) {
            throw new IllegalStateException(
                    "Impossible de repasser en PENDING un paiement déjà dans l'état " + this.status);
        }
        this.status = PaymentStatus.PENDING;
        this.gatewayReference = gatewayReference;
        this.updatedAt = Instant.now();
    }

    /**
     * Passe le paiement en attente d'une étape d'autorisation synchrone (PIN carte, AVS, ou
     * redirection 3DS) avant de pouvoir être finalisé - contrairement à {@link #markPending}, qui
     * couvre l'attente d'une confirmation asynchrone (webhook mobile money/PayPal). Autorisé aussi
     * depuis PENDING_AUTHORIZATION lui-même : Flutterwave peut chaîner un second défi après le
     * premier (ex: PIN accepté puis OTP exigé), ce qui remplace simplement le défi en cours par le
     * suivant plutôt que de représenter un changement d'état invalide.
     */
    public void markPendingAuthorization(String gatewayReference, PaymentAuthorizationType authorizationType,
                                         String redirectUrl) {
        if (this.status != PaymentStatus.PENDING && this.status != PaymentStatus.PENDING_AUTHORIZATION) {
            throw new IllegalStateException(
                    "Impossible de passer en PENDING_AUTHORIZATION un paiement déjà dans l'état " + this.status);
        }
        this.status = PaymentStatus.PENDING_AUTHORIZATION;
        this.gatewayReference = gatewayReference;
        this.authorizationType = authorizationType;
        this.authorizationRedirectUrl = redirectUrl;
        this.updatedAt = Instant.now();
    }

    public void markSucceeded(String gatewayReference) {
        if (this.status == PaymentStatus.SUCCEEDED) {
            return; // idempotence : un callback dupliqué ne doit pas re-déclencher d'erreur
        }
        if (this.status == PaymentStatus.FAILED) {
            throw new IllegalStateException("Impossible de marquer comme réussi un paiement déjà FAILED");
        }
        this.status = PaymentStatus.SUCCEEDED;
        this.gatewayReference = gatewayReference;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String reason) {
        if (this.status == PaymentStatus.SUCCEEDED) {
            throw new IllegalStateException("Impossible de marquer comme échoué un paiement déjà SUCCEEDED");
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getBookingId() {
        return bookingId;
    }

    public Money getAmount() {
        return amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getPayerReferenceLast4() {
        return payerReferenceLast4;
    }

    public boolean isDepositPayment() {
        return depositPayment;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getGatewayReference() {
        return gatewayReference;
    }

    public PaymentAuthorizationType getAuthorizationType() {
        return authorizationType;
    }

    public String getAuthorizationRedirectUrl() {
        return authorizationRedirectUrl;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }


}