package com.guentours.payment.service;

import com.guentours.booking.BookingService;
import com.guentours.booking.domain.BookingStatus;
import com.guentours.booking.domain.BookingSummary;
import com.guentours.booking.domain.PaymentPlan;
import com.guentours.payment.domain.Payment;
import com.guentours.payment.domain.PaymentAuthorizationType;
import com.guentours.payment.domain.PaymentMethod;
import com.guentours.payment.domain.PaymentRepository;
import com.guentours.payment.domain.PaymentStatus;
import com.guentours.payment.events.BookingDepositPaidEvent;
import com.guentours.payment.events.BookingFullyPaidEvent;
import com.guentours.payment.events.PaymentFailedEvent;
import com.guentours.payment.gateway.AuthorizationChallenge;
import com.guentours.payment.gateway.ChargeRequest;
import com.guentours.payment.gateway.ChargeResult;
import com.guentours.payment.gateway.PaymentGateway;
import com.guentours.payment.gateway.PendingCardAuthorizationCache;
import com.guentours.payment.web.PaymentRequest;
import com.guentours.shared.Money;
import com.guentours.shared.exception.BusinessException;
import com.guentours.shared.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProviderRoutingService routingService;
    private final BookingService bookingService;
    private final ApplicationEventPublisher eventPublisher;
    private final PendingCardAuthorizationCache pendingCardAuthorizationCache;

    public PaymentService(PaymentRepository paymentRepository, PaymentProviderRoutingService routingService,
                          BookingService bookingService, ApplicationEventPublisher eventPublisher,
                          PendingCardAuthorizationCache pendingCardAuthorizationCache) {
        this.paymentRepository = paymentRepository;
        this.routingService = routingService;
        this.bookingService = bookingService;
        this.eventPublisher = eventPublisher;
        this.pendingCardAuthorizationCache = pendingCardAuthorizationCache;
    }

    /**
     * Initie un paiement. Le Payment est toujours persisté en PENDING avant l'appel au gateway,
     * pour garder une trace même en cas de crash/timeout réseau pendant l'appel.
     * Le résultat final (succès/échec) peut arriver immédiatement (carte sans 3DS, réponse synchrone)
     * ou plus tard via webhook (mobile money, carte avec redirection/PIN, PayPal...) — voir confirmFromGatewayCallback.
     */
    @Transactional
    public Payment pay(PaymentRequest request) {
        BookingSummary booking = bookingService.getSummary(request.bookingId());
        if (booking.status() != BookingStatus.PENDING_PAYMENT && booking.status() != BookingStatus.DEPOSIT_PAID) {
            throw new BusinessException("Booking " + booking.id() + " is not awaiting payment");
        }

        boolean isDepositPayment = booking.status() == BookingStatus.PENDING_PAYMENT
                && booking.paymentPlan() == PaymentPlan.PAY_LATER;

        validatePaymentMethodFields(request);

        // Résolu avant de créer le Payment : une route désactivée par l'admin (ou un fournisseur
        // configuré mais jamais déployé) doit rejeter la tentative immédiatement plutôt que de
        // créer un Payment PENDING qui basculerait FAILED juste après.
        PaymentGateway gateway = routingService.resolveGateway(request.countryCode(), request.paymentMethod());

        Money amountDue = booking.amountDue();

        Payment payment = new Payment(booking.id(), amountDue, request.paymentMethod(),
                payerReferenceLast4(request), isDepositPayment, request.countryCode(), request.countryCurrency());
        paymentRepository.save(payment);

        ChargeRequest chargeRequest = new ChargeRequest(amountDue.amount(), amountDue.currency(),
                request.countryCode(), request.countryCurrency(), request.paymentMethod(),
                request.cardNumber(), request.cardHolderName(), request.expiry(), request.cvv(),
                request.mobileNumber(), booking.contactEmail(), payment.getId(),
                request.customerIp(), request.billingAddress()); // billingAddress non porté par PaymentRequest pour l'instant

        ChargeResult result;
        try {
            result = gateway.charge(chargeRequest);
        } catch (Exception ex) {
            log.error("Erreur lors de l'appel au gateway pour le payment {} (booking {})",
                    payment.getId(), booking.id(), ex);
            failPayment(payment, "Erreur technique gateway : " + ex.getMessage());
            return payment;
        }

        applyChargeResult(payment, booking, result, chargeRequest);
        return payment;
    }

    /**
     * Soumet le PIN carte demandé par le gateway pour un paiement en {@code PENDING_AUTHORIZATION},
     * en réutilisant les détails de carte mis en cache par {@link #pay} - jamais persistés en base.
     * Si le cache a expiré (le payeur a mis trop de temps), le paiement est marqué en échec avec un
     * message explicite plutôt que de rester bloqué indéfiniment.
     */
    @Transactional
    public Payment completeCardPinAuthorization(String paymentId, String pin) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment introuvable : " + paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING_AUTHORIZATION
                || payment.getAuthorizationType() != PaymentAuthorizationType.PIN) {
            throw new BusinessException("Ce paiement n'est pas en attente d'un code PIN.");
        }

        ChargeRequest originalRequest = pendingCardAuthorizationCache.take(paymentId).orElse(null);
        if (originalRequest == null) {
            failPayment(payment, "Session d'autorisation expirée, veuillez recommencer le paiement.");
            return payment;
        }

        ChargeResult result;
        try {
            PaymentGateway gateway = routingService.resolveGateway(payment.getCountryCode(), payment.getPaymentMethod());
            result = gateway.completeCardPinAuthorization(paymentId, originalRequest, pin);
        } catch (Exception ex) {
            log.error("Erreur lors de la validation du PIN pour le payment {}", paymentId, ex);
            failPayment(payment, "Erreur technique gateway : " + ex.getMessage());
            return payment;
        }

        BookingSummary booking = bookingService.getSummary(payment.getBookingId());
        // originalRequest est repassé ici (pas le surcharge à 3 arguments) car Flutterwave peut
        // chaîner un nouveau défi PIN (mauvais code, re-tentative) après celui-ci : sans le
        // recacher, la tentative suivante trouverait le cache déjà vidé et échouerait à tort avec
        // "session expirée" alors que la carte est toujours en cours d'authentification.
        applyChargeResult(payment, booking, result, originalRequest);
        return payment;
    }

    /**
     * Point d'entrée appelé par le webhook du gateway lorsqu'un paiement initialement PENDING
     * (ou PENDING_AUTHORIZATION) est finalement confirmé ou rejeté.
     * Idempotent : si le payment n'est plus dans un état en attente, l'appel est ignoré
     * (retry webhook, double notification, etc.).
     */
    @Transactional
    public void confirmFromGatewayCallback(String paymentId, ChargeResult result) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment introuvable : " + paymentId));

        if (payment.getStatus() == PaymentStatus.SUCCEEDED || payment.getStatus() == PaymentStatus.FAILED) {
            log.warn("Callback gateway reçu pour le payment {} déjà dans l'état {}, ignoré (idempotence).",
                    payment.getId(), payment.getStatus());
            return;
        }

        BookingSummary booking = bookingService.getSummary(payment.getBookingId());
        applyChargeResult(payment, booking, result);
    }

    /** Overload used by the webhook path, which never has the original card details to cache. */
    private void applyChargeResult(Payment payment, BookingSummary booking, ChargeResult result) {
        applyChargeResult(payment, booking, result, null);
    }

    /**
     * Applique le résultat d'une charge (qu'elle vienne du flux synchrone initial, d'une validation
     * de PIN, ou d'un callback webhook) : transition du Payment, puis du Booking et publication
     * d'event si le paiement est confirmé. {@code originalRequest} n'est fourni (non-null) que par
     * {@link #pay}, pour mettre en cache les détails carte le temps d'une éventuelle autorisation PIN.
     */
    private void applyChargeResult(Payment payment, BookingSummary booking, ChargeResult result,
                                   ChargeRequest originalRequest) {
        if (result.isSucceeded()) {
            payment.markSucceeded(result.gatewayReference());
            paymentRepository.save(payment);
            applyConfirmedPayment(payment, booking);

        } else if (result.isFailed()) {
            failPayment(payment, result.failureReason());

        } else if (result.requiresAuthorization()) {
            AuthorizationChallenge challenge = result.authorizationChallenge();
            PaymentAuthorizationType authorizationType = switch (challenge.type()) {
                case PIN -> PaymentAuthorizationType.PIN;
                case AVS -> PaymentAuthorizationType.AVS;
                case REDIRECT -> PaymentAuthorizationType.REDIRECT;
                case OTP -> PaymentAuthorizationType.OTP;
            };

            if (authorizationType == PaymentAuthorizationType.AVS) {
                // L'AVS a besoin de l'adresse de facturation, que le formulaire carte ne collecte pas
                // aujourd'hui (elle n'est demandée que pour Google/Apple Pay et PayPal) : on échoue
                // proprement plutôt que de rester bloqué sans jamais pouvoir compléter le défi.
                failPayment(payment, "Ce mode de vérification carte (AVS) n'est pas encore supporté - "
                        + "veuillez réessayer avec un autre moyen de paiement.");
                return;
            }

            if (authorizationType == PaymentAuthorizationType.OTP) {
                // La validation OTP passe par un appel gateway distinct (ValidateCharge, corrélé par
                // le flw_ref de Flutterwave) que ce paiement ne sait pas encore soumettre : on échoue
                // proprement plutôt que de laisser le paiement PENDING_AUTHORIZATION sans jamais
                // pouvoir en sortir.
                failPayment(payment, "Ce mode de vérification carte (OTP) n'est pas encore supporté - "
                        + "veuillez réessayer avec un autre moyen de paiement.");
                return;
            }

            payment.markPendingAuthorization(result.gatewayReference(), authorizationType, challenge.redirectUrl());
            paymentRepository.save(payment);
            if (authorizationType == PaymentAuthorizationType.PIN && originalRequest != null) {
                pendingCardAuthorizationCache.put(payment.getId(), originalRequest);
            }
            log.info("Payment {} en attente d'autorisation ({}), ref gateway = {}",
                    payment.getId(), authorizationType, result.gatewayReference());

        } else {
            // PENDING : attente d'un webhook asynchrone (mobile money, Google/Apple Pay, PayPal).
            payment.markPending(result.gatewayReference());
            paymentRepository.save(payment);
            log.info("Payment {} en attente ({}), ref gateway = {}",
                    payment.getId(), result.status(), result.gatewayReference());
        }
    }

    /**
     * Applique les effets métier d'un paiement confirmé avec succès : transition du booking
     * et publication de l'événement correspondant.
     * Rappel : seul le paiement complet (BookingFullyPaidEvent) déclenche la commission revendeur —
     * un acompte ne génère jamais de commission.
     */
    private void applyConfirmedPayment(Payment payment, BookingSummary booking) {
        if (payment.isDepositPayment()) {
            bookingService.markDepositPaid(booking.id());
            eventPublisher.publishEvent(new BookingDepositPaidEvent(
                    booking.id(), payment.getId(), payment.getGatewayReference()));
        } else {
            bookingService.markPaidAndConfirm(booking.id(), payment.getGatewayReference(),
                    payment.getPayerReferenceLast4());
            eventPublisher.publishEvent(new BookingFullyPaidEvent(
                    booking.id(), payment.getId(), payment.getGatewayReference(),
                    payment.getPayerReferenceLast4()));
        }
    }

    private void failPayment(Payment payment, String reason) {
        payment.markFailed(reason);
        paymentRepository.save(payment);
        eventPublisher.publishEvent(new PaymentFailedEvent(payment.getBookingId(), payment.getId(), reason));
    }

    public Payment getById(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + paymentId));
    }

    private String payerReferenceLast4(PaymentRequest request) {
        return request.paymentMethod() == PaymentMethod.CARD && request.cardNumber() != null
                ? request.cardNumber().substring(Math.max(0, request.cardNumber().length() - 4))
                : request.mobileNumber();
    }

    private void validatePaymentMethodFields(PaymentRequest request) {
        switch (request.paymentMethod()) {
            case CARD -> {
                if (isBlank(request.cardNumber()) || !request.cardNumber().matches("\\d{12,19}")) {
                    throw new BusinessException("cardNumber must be 12-19 digits");
                }
                if (isBlank(request.cardHolderName())) {
                    throw new BusinessException("cardHolderName is required");
                }
                if (isBlank(request.expiry()) || !request.expiry().matches("(0[1-9]|1[0-2])/\\d{2}")) {
                    throw new BusinessException("expiry must be MM/YY");
                }
                if (isBlank(request.cvv()) || !request.cvv().matches("\\d{3,4}")) {
                    throw new BusinessException("cvv must be 3-4 digits");
                }
            }
            case MOBILE_MONEY -> {
                if (isBlank(request.mobileNumber()) || !request.mobileNumber().matches("\\+?\\d{8,15}")) {
                    throw new BusinessException("mobileNumber must be a valid phone number");
                }
            }
            case GOOGLE_PAY, APPLE_PAY, PAYPAL -> {
                // Pas de champ carte/mobile à valider ici : ces moyens de paiement redirigent ou
                // fournissent un token généré côté client. Leurs prérequis spécifiques (adresse de
                // facturation, token JS...) sont validés au niveau du gateway concerné, pas ici.
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}