package com.guentours.payment.service;

import com.guentours.booking.BookingService;
import com.guentours.booking.domain.BookingStatus;
import com.guentours.booking.domain.BookingSummary;
import com.guentours.booking.domain.PaymentPlan;
import com.guentours.payment.domain.Payment;
import com.guentours.payment.domain.PaymentMethod;
import com.guentours.payment.domain.PaymentRepository;
import com.guentours.payment.domain.PaymentStatus;
import com.guentours.payment.events.BookingDepositPaidEvent;
import com.guentours.payment.events.BookingFullyPaidEvent;
import com.guentours.payment.gateway.ChargeRequest;
import com.guentours.payment.gateway.ChargeResult;
import com.guentours.payment.gateway.PaymentGateway;
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
    private final PaymentGateway paymentGateway;
    private final BookingService bookingService;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentService(PaymentRepository paymentRepository, PaymentGateway paymentGateway,
                          BookingService bookingService, ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.bookingService = bookingService;
        this.eventPublisher = eventPublisher;
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
            result = paymentGateway.charge(chargeRequest);
        } catch (Exception ex) {
            log.error("Erreur lors de l'appel au gateway pour le payment {} (booking {})",
                    payment.getId(), booking.id(), ex);
            payment.markFailed("Erreur technique gateway : " + ex.getMessage());
            paymentRepository.save(payment);
            return payment;
        }

        applyChargeResult(payment, booking, result);
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

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("Callback gateway reçu pour le payment {} déjà dans l'état {}, ignoré (idempotence).",
                    payment.getId(), payment.getStatus());
            return;
        }

        BookingSummary booking = bookingService.getSummary(payment.getBookingId());
        applyChargeResult(payment, booking, result);
    }

    /**
     * Applique le résultat d'une charge (qu'il vienne du flux synchrone ou d'un callback) :
     * transition du Payment, puis du Booking et publication d'event si le paiement est confirmé.
     */
    private void applyChargeResult(Payment payment, BookingSummary booking, ChargeResult result) {
        if (result.isSucceeded()) {
            payment.markSucceeded(result.gatewayReference());
            paymentRepository.save(payment);
            applyConfirmedPayment(payment, booking);

        } else if (result.isFailed()) {
            payment.markFailed(result.failureReason());
            paymentRepository.save(payment);

        } else {
            // PENDING (attente webhook : mobile money, Google/Apple Pay, PayPal) ou
            // PENDING_AUTHORIZATION (attente PIN/OTP/redirection carte)
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