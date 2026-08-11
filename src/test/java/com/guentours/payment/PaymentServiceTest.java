package com.guentours.payment;

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
import com.guentours.payment.gateway.ChargeStatus;
import com.guentours.payment.gateway.PaymentGateway;
import com.guentours.payment.gateway.PendingCardAuthorizationCache;
import com.guentours.payment.service.PaymentProviderRoutingService;
import com.guentours.payment.service.PaymentService;
import com.guentours.payment.web.PaymentRequest;
import com.guentours.shared.Money;
import com.guentours.shared.exception.BusinessException;
import com.guentours.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String BOOKING_ID = "booking-123";
    private static final String CONTACT_EMAIL = "jean.dupont@example.com";
    private static final Money PRICE = Money.of(150_000, "XAF");
    private static final Money RESERVATION_FEE = Money.of(30_000, "XAF");

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private PaymentProviderRoutingService routingService;
    @Mock
    private BookingService bookingService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PaymentService paymentService;
    private PendingCardAuthorizationCache pendingCardAuthorizationCache;

    @BeforeEach
    void setUp() {
        pendingCardAuthorizationCache = new PendingCardAuthorizationCache();
        paymentService = new PaymentService(paymentRepository, routingService, bookingService, eventPublisher,
                pendingCardAuthorizationCache);
        lenient().when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // Ces tests portent sur le comportement de PaymentService une fois le gateway obtenu, pas
        // sur le routage lui-même (voir PaymentProviderRoutingServiceTest) : par défaut on résout
        // toujours vers le même gateway mocké, quel que soit le pays/mode demandé.
        lenient().when(routingService.resolveGateway(any(), any())).thenReturn(paymentGateway);
    }

    /**
     * The mocked repository never runs real JPA, so {@code Payment.id} (a {@code @GeneratedValue})
     * stays null across saves by default - fine for tests that never key anything off it, but the
     * card-authorization cache is keyed by payment id, so these tests need one assigned like a real
     * persist would.
     */
    private void assignGeneratedIdOnSave(String id) {
        lenient().when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            try {
                var idField = Payment.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(payment, id);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            return payment;
        });
    }

    private BookingSummary fullPaymentBooking() {
        return new BookingSummary(BOOKING_ID, "user-1", CONTACT_EMAIL, BookingStatus.PENDING_PAYMENT,
                PRICE, PaymentPlan.PAY_NOW, Money.zero("XAF"), PRICE);
    }

    private BookingSummary depositEligibleBooking() {
        return new BookingSummary(BOOKING_ID, "user-1", CONTACT_EMAIL, BookingStatus.PENDING_PAYMENT,
                PRICE, PaymentPlan.PAY_LATER, RESERVATION_FEE, RESERVATION_FEE);
    }

    private BookingSummary balanceDueBooking() {
        Money balance = PRICE.subtract(RESERVATION_FEE);
        return new BookingSummary(BOOKING_ID, "user-1", CONTACT_EMAIL, BookingStatus.DEPOSIT_PAID,
                PRICE, PaymentPlan.PAY_LATER, RESERVATION_FEE, balance);
    }

    private PaymentRequest cardRequest() {
        return new PaymentRequest(BOOKING_ID, PaymentMethod.CARD, "CM", "XAF", "4242424242424242",
                "Jean Dupont", "12/28", "123", null, null, null);
    }

    @Nested
    @DisplayName("pay() - validations préalables")
    class Validation {

        @Test
        @DisplayName("rejette un booking qui n'est pas en attente de paiement")
        void shouldRejectBookingNotAwaitingPayment() {
            BookingSummary confirmedBooking = new BookingSummary(BOOKING_ID, "user-1", CONTACT_EMAIL,
                    BookingStatus.CONFIRMED, PRICE, PaymentPlan.PAY_NOW, Money.zero("XAF"), PRICE);
            when(bookingService.getSummary(BOOKING_ID)).thenReturn(confirmedBooking);

            assertThatThrownBy(() -> paymentService.pay(cardRequest()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("not awaiting payment");

            verifyNoInteractions(paymentGateway, paymentRepository, eventPublisher);
        }

        @Test
        @DisplayName("rejette une carte avec un CVV invalide")
        void shouldRejectInvalidCvv() {
            when(bookingService.getSummary(BOOKING_ID)).thenReturn(fullPaymentBooking());
            PaymentRequest invalidRequest = new PaymentRequest(BOOKING_ID, PaymentMethod.CARD,
                    "CM", "XAF", "4242424242424242", "Jean Dupont", "12/28", "12", null, null, null);

            assertThatThrownBy(() -> paymentService.pay(invalidRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("cvv");

            verifyNoInteractions(paymentGateway);
        }

        @Test
        @DisplayName("rejette un mobile money sans numéro valide")
        void shouldRejectInvalidMobileNumber() {
            when(bookingService.getSummary(BOOKING_ID)).thenReturn(fullPaymentBooking());
            PaymentRequest invalidRequest = new PaymentRequest(BOOKING_ID, PaymentMethod.MOBILE_MONEY,
                    "CM", "XAF", null, null, null, null, "abc", null, null);

            assertThatThrownBy(() -> paymentService.pay(invalidRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("mobileNumber");
        }
    }

    @Nested
    @DisplayName("pay() - paiement complet réussi")
    class FullPaymentSucceeded {

        @Test
        @DisplayName("confirme le booking et publie BookingFullyPaidEvent (pas de deposit event)")
        void shouldConfirmBookingAndPublishFullyPaidEvent() {
            when(bookingService.getSummary(BOOKING_ID)).thenReturn(fullPaymentBooking());
            when(paymentGateway.charge(any(ChargeRequest.class))).thenReturn(
                    new ChargeResult(ChargeStatus.SUCCEEDED, "flw-ref-001", "4242", null, null));

            Payment result = paymentService.pay(cardRequest());

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
            assertThat(result.getGatewayReference()).isEqualTo("flw-ref-001");
            assertThat(result.isDepositPayment()).isFalse();

            verify(bookingService).markPaidAndConfirm(BOOKING_ID, "flw-ref-001", "4242");
            verify(bookingService, never()).markDepositPaid(anyString());

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue()).isInstanceOf(BookingFullyPaidEvent.class);
            BookingFullyPaidEvent event = (BookingFullyPaidEvent) eventCaptor.getValue();
            assertThat(event.bookingId()).isEqualTo(BOOKING_ID);
            assertThat(event.gatewayReference()).isEqualTo("flw-ref-001");
        }

        @Test
        @DisplayName("le ChargeRequest transmis au gateway porte le contactEmail du booking")
        void shouldForwardBookingContactEmailToGateway() {
            when(bookingService.getSummary(BOOKING_ID)).thenReturn(fullPaymentBooking());
            when(paymentGateway.charge(any(ChargeRequest.class))).thenReturn(
                    new ChargeResult(ChargeStatus.SUCCEEDED, "flw-ref-001", "4242", null, null));

            paymentService.pay(cardRequest());

            ArgumentCaptor<ChargeRequest> chargeCaptor = ArgumentCaptor.forClass(ChargeRequest.class);
            verify(paymentGateway).charge(chargeCaptor.capture());
            assertThat(chargeCaptor.getValue().customerEmail()).isEqualTo(CONTACT_EMAIL);
            assertThat(chargeCaptor.getValue().currency()).isEqualTo("XAF");
        }

        @Test
        @DisplayName("le Payment est persisté en PENDING avant l'appel gateway")
        void shouldPersistPendingBeforeGatewayCall() {
            when(bookingService.getSummary(BOOKING_ID)).thenReturn(fullPaymentBooking());
            when(paymentGateway.charge(any(ChargeRequest.class))).thenReturn(
                    new ChargeResult(ChargeStatus.SUCCEEDED, "flw-ref-001", "4242", null, null));

            // Payment is a mutable entity reused across both save() calls, so an ArgumentCaptor would
            // only ever see its final state - snapshot the status at the time of each call instead.
            List<PaymentStatus> savedStatuses = new ArrayList<>();
            lenient().when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment payment = invocation.getArgument(0);
                savedStatuses.add(payment.getStatus());
                return payment;
            });

            paymentService.pay(cardRequest());

            assertThat(savedStatuses).hasSize(2);
            assertThat(savedStatuses.get(0)).isEqualTo(PaymentStatus.PENDING);
            assertThat(savedStatuses.get(1)).isEqualTo(PaymentStatus.SUCCEEDED);
        }
    }

    @Nested
    @DisplayName("pay() - acompte réussi")
    class DepositPaymentSucceeded {

        @Test
        @DisplayName("marque l'acompte payé et publie BookingDepositPaidEvent (pas de commission)")
        void shouldMarkDepositPaidAndPublishDepositEvent() {
            when(bookingService.getSummary(BOOKING_ID)).thenReturn(depositEligibleBooking());
            when(paymentGateway.charge(any(ChargeRequest.class))).thenReturn(
                    new ChargeResult(ChargeStatus.SUCCEEDED, "flw-ref-002", "4242", null, null));

            Payment result = paymentService.pay(cardRequest());

            assertThat(result.isDepositPayment()).isTrue();
            verify(bookingService).markDepositPaid(BOOKING_ID);
            verify(bookingService, never()).markPaidAndConfirm(anyString(), anyString(), anyString());

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue()).isInstanceOf(BookingDepositPaidEvent.class);
        }

        @Test
        @DisplayName("le paiement du solde après acompte est traité comme un paiement complet")
        void shouldTreatBalancePaymentAsFullPayment() {
            when(bookingService.getSummary(BOOKING_ID)).thenReturn(balanceDueBooking());
            when(paymentGateway.charge(any(ChargeRequest.class))).thenReturn(
                    new ChargeResult(ChargeStatus.SUCCEEDED, "flw-ref-003", "4242", null, null));

            Payment result = paymentService.pay(cardRequest());

            assertThat(result.isDepositPayment()).isFalse();
            verify(bookingService).markPaidAndConfirm(BOOKING_ID, "flw-ref-003", "4242");

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue()).isInstanceOf(BookingFullyPaidEvent.class);
        }
    }

    @Nested
    @DisplayName("pay() - échec et cas asynchrones")
    class FailureAndAsyncCases {

        @Test
        @DisplayName("paiement refusé par le gateway : Payment FAILED, PaymentFailedEvent publié")
        void shouldMarkFailedWhenGatewayRejects() {
            when(bookingService.getSummary(BOOKING_ID)).thenReturn(fullPaymentBooking());
            when(paymentGateway.charge(any(ChargeRequest.class))).thenReturn(
                    new ChargeResult(ChargeStatus.FAILED, null, "4242", "Fonds insuffisants", null));

            Payment result = paymentService.pay(cardRequest());

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(result.getFailureReason()).isEqualTo("Fonds insuffisants");
            verify(eventPublisher).publishEvent(new PaymentFailedEvent(BOOKING_ID, result.getId(), "Fonds insuffisants"));
            verify(bookingService, never()).markPaidAndConfirm(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("gateway en PENDING (mobile money) : Payment reste PENDING, aucun event publié")
        void shouldStayPendingForAsyncGateway() {
            when(bookingService.getSummary(BOOKING_ID)).thenReturn(fullPaymentBooking());
            when(paymentGateway.charge(any(ChargeRequest.class))).thenReturn(
                    new ChargeResult(ChargeStatus.PENDING, "flw-ref-004", "4242", null, null));

            Payment result = paymentService.pay(cardRequest());

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(result.getGatewayReference()).isEqualTo("flw-ref-004");
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("exception réseau/gateway : Payment marqué FAILED plutôt que perdu")
        void shouldMarkFailedOnGatewayException() {
            when(bookingService.getSummary(BOOKING_ID)).thenReturn(fullPaymentBooking());
            when(paymentGateway.charge(any(ChargeRequest.class)))
                    .thenThrow(new RuntimeException("Timeout gateway"));

            Payment result = paymentService.pay(cardRequest());

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(result.getFailureReason()).contains("Timeout gateway");
            ArgumentCaptor<PaymentFailedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentFailedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().bookingId()).isEqualTo(BOOKING_ID);
            assertThat(eventCaptor.getValue().reason()).contains("Timeout gateway");
        }
    }

    @Nested
    @DisplayName("pay() - carte exigeant une autorisation (PIN/AVS/REDIRECT)")
    class CardAuthorizationChallenge {

        @Test
        @DisplayName("PIN demandé : Payment en PENDING_AUTHORIZATION, détails carte mis en cache")
        void shouldMarkPendingAuthorizationAndCacheCardDetailsForPin() {
            assignGeneratedIdOnSave("payment-pin-challenge");
            when(bookingService.getSummary(BOOKING_ID)).thenReturn(fullPaymentBooking());
            when(paymentGateway.charge(any(ChargeRequest.class))).thenReturn(ChargeResult.pendingAuthorization(
                    "flw-ref-pin", new AuthorizationChallenge(AuthorizationChallenge.AuthorizationType.PIN, null)));

            Payment result = paymentService.pay(cardRequest());

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING_AUTHORIZATION);
            assertThat(result.getAuthorizationType()).isEqualTo(PaymentAuthorizationType.PIN);
            assertThat(pendingCardAuthorizationCache.take("payment-pin-challenge")).isPresent();
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("REDIRECT demandé : Payment en PENDING_AUTHORIZATION avec l'URL de redirection, rien en cache")
        void shouldMarkPendingAuthorizationWithRedirectUrl() {
            assignGeneratedIdOnSave("payment-redirect-challenge");
            when(bookingService.getSummary(BOOKING_ID)).thenReturn(fullPaymentBooking());
            when(paymentGateway.charge(any(ChargeRequest.class))).thenReturn(ChargeResult.pendingAuthorization(
                    "flw-ref-3ds", new AuthorizationChallenge(
                            AuthorizationChallenge.AuthorizationType.REDIRECT, "https://bank.example/3ds")));

            Payment result = paymentService.pay(cardRequest());

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING_AUTHORIZATION);
            assertThat(result.getAuthorizationType()).isEqualTo(PaymentAuthorizationType.REDIRECT);
            assertThat(result.getAuthorizationRedirectUrl()).isEqualTo("https://bank.example/3ds");
            assertThat(pendingCardAuthorizationCache.take("payment-redirect-challenge")).isEmpty();
        }

        @Test
        @DisplayName("AVS demandé : marqué FAILED immédiatement (pas d'adresse de facturation collectée pour CARD)")
        void shouldFailImmediatelyOnAvsChallenge() {
            when(bookingService.getSummary(BOOKING_ID)).thenReturn(fullPaymentBooking());
            when(paymentGateway.charge(any(ChargeRequest.class))).thenReturn(ChargeResult.pendingAuthorization(
                    "flw-ref-avs", new AuthorizationChallenge(AuthorizationChallenge.AuthorizationType.AVS, null)));

            Payment result = paymentService.pay(cardRequest());

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
            verify(eventPublisher).publishEvent(any(PaymentFailedEvent.class));
        }

        @Test
        @DisplayName("OTP demandé (ex: chaîné après un PIN accepté) : marqué FAILED immédiatement, non supporté")
        void shouldFailImmediatelyOnOtpChallenge() {
            when(bookingService.getSummary(BOOKING_ID)).thenReturn(fullPaymentBooking());
            when(paymentGateway.charge(any(ChargeRequest.class))).thenReturn(ChargeResult.pendingAuthorization(
                    "flw-ref-otp", new AuthorizationChallenge(AuthorizationChallenge.AuthorizationType.OTP, null)));

            Payment result = paymentService.pay(cardRequest());

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
            verify(eventPublisher).publishEvent(any(PaymentFailedEvent.class));
        }
    }

    @Nested
    @DisplayName("completeCardPinAuthorization()")
    class CompleteCardPinAuthorization {

        private Payment pendingAuthorizationPayment() {
            Payment payment = new Payment(BOOKING_ID, PRICE, PaymentMethod.CARD, "4242", false, "CM", "XAF");
            payment.markPendingAuthorization("flw-ref-pin", PaymentAuthorizationType.PIN, null);
            return payment;
        }

        @Test
        @DisplayName("PIN correct : Payment SUCCEEDED et booking confirmé")
        void shouldSucceedAndConfirmBooking() {
            Payment payment = pendingAuthorizationPayment();
            pendingCardAuthorizationCache.put("payment-pin",
                    new ChargeRequest(PRICE.amount(), "XAF", "CM", "XAF", PaymentMethod.CARD,
                            "4242424242424242", "Jean Dupont", "12/28", "123", null, CONTACT_EMAIL,
                            "payment-pin", null, null));
            when(paymentRepository.findById("payment-pin")).thenReturn(Optional.of(payment));
            when(bookingService.getSummary(BOOKING_ID)).thenReturn(fullPaymentBooking());
            when(paymentGateway.completeCardPinAuthorization(eq("payment-pin"), any(ChargeRequest.class), eq("1234")))
                    .thenReturn(ChargeResult.success("flw-ref-pin-confirmed", "4242"));

            Payment result = paymentService.completeCardPinAuthorization("payment-pin", "1234");

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
            verify(bookingService).markPaidAndConfirm(BOOKING_ID, "flw-ref-pin-confirmed", "4242");
        }

        @Test
        @DisplayName("PIN incorrect : Payment FAILED")
        void shouldFailOnWrongPin() {
            Payment payment = pendingAuthorizationPayment();
            pendingCardAuthorizationCache.put("payment-pin",
                    new ChargeRequest(PRICE.amount(), "XAF", "CM", "XAF", PaymentMethod.CARD,
                            "4242424242424242", "Jean Dupont", "12/28", "123", null, CONTACT_EMAIL,
                            "payment-pin", null, null));
            when(paymentRepository.findById("payment-pin")).thenReturn(Optional.of(payment));
            when(bookingService.getSummary(BOOKING_ID)).thenReturn(fullPaymentBooking());
            when(paymentGateway.completeCardPinAuthorization(eq("payment-pin"), any(ChargeRequest.class), eq("0000")))
                    .thenReturn(ChargeResult.declined("Incorrect PIN"));

            Payment result = paymentService.completeCardPinAuthorization("payment-pin", "0000");

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
            verify(eventPublisher).publishEvent(any(PaymentFailedEvent.class));
        }

        @Test
        @DisplayName("cache expiré/absent : Payment FAILED avec message explicite, gateway jamais rappelé")
        void shouldFailWhenCacheMissing() {
            Payment payment = pendingAuthorizationPayment();
            when(paymentRepository.findById("payment-pin")).thenReturn(Optional.of(payment));

            Payment result = paymentService.completeCardPinAuthorization("payment-pin", "1234");

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(result.getFailureReason()).contains("expirée");
            verifyNoInteractions(paymentGateway);
            ArgumentCaptor<PaymentFailedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentFailedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().reason()).contains("expirée");
        }

        @Test
        @DisplayName("rejette un paiement qui n'attend pas de PIN")
        void shouldRejectWhenNotAwaitingPin() {
            Payment succeeded = new Payment(BOOKING_ID, PRICE, PaymentMethod.CARD, "4242", false, "CM", "XAF");
            succeeded.markSucceeded("flw-ref");
            when(paymentRepository.findById("payment-done")).thenReturn(Optional.of(succeeded));

            assertThatThrownBy(() -> paymentService.completeCardPinAuthorization("payment-done", "1234"))
                    .isInstanceOf(BusinessException.class);

            verifyNoInteractions(paymentGateway);
        }

        @Test
        @DisplayName("PIN accepté mais Flutterwave enchaîne un OTP : pas d'exception, Payment FAILED proprement")
        void shouldNotThrowWhenGatewayChainsAnotherChallenge() {
            Payment payment = pendingAuthorizationPayment();
            pendingCardAuthorizationCache.put("payment-pin",
                    new ChargeRequest(PRICE.amount(), "XAF", "CM", "XAF", PaymentMethod.CARD,
                            "4242424242424242", "Jean Dupont", "12/28", "123", null, CONTACT_EMAIL,
                            "payment-pin", null, null));
            when(paymentRepository.findById("payment-pin")).thenReturn(Optional.of(payment));
            when(bookingService.getSummary(BOOKING_ID)).thenReturn(fullPaymentBooking());
            when(paymentGateway.completeCardPinAuthorization(eq("payment-pin"), any(ChargeRequest.class), eq("1234")))
                    .thenReturn(ChargeResult.pendingAuthorization("flw-ref-otp",
                            new AuthorizationChallenge(AuthorizationChallenge.AuthorizationType.OTP, null)));

            Payment result = paymentService.completeCardPinAuthorization("payment-pin", "1234");

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
            verify(eventPublisher).publishEvent(any(PaymentFailedEvent.class));
        }

        @Test
        @DisplayName("PIN accepté mais Flutterwave redemande un PIN : Payment reste PENDING_AUTHORIZATION, cache repeuplé")
        void shouldStayPendingAuthorizationAndRecacheWhenGatewayAsksForAnotherPin() {
            assignGeneratedIdOnSave("payment-pin");
            Payment payment = pendingAuthorizationPayment();
            var cachedRequest = new ChargeRequest(PRICE.amount(), "XAF", "CM", "XAF", PaymentMethod.CARD,
                    "4242424242424242", "Jean Dupont", "12/28", "123", null, CONTACT_EMAIL,
                    "payment-pin", null, null);
            pendingCardAuthorizationCache.put("payment-pin", cachedRequest);
            when(paymentRepository.findById("payment-pin")).thenReturn(Optional.of(payment));
            when(bookingService.getSummary(BOOKING_ID)).thenReturn(fullPaymentBooking());
            when(paymentGateway.completeCardPinAuthorization(eq("payment-pin"), any(ChargeRequest.class), eq("0000")))
                    .thenReturn(ChargeResult.pendingAuthorization("flw-ref-pin-2",
                            new AuthorizationChallenge(AuthorizationChallenge.AuthorizationType.PIN, null)));

            Payment result = paymentService.completeCardPinAuthorization("payment-pin", "0000");

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING_AUTHORIZATION);
            assertThat(result.getAuthorizationType()).isEqualTo(PaymentAuthorizationType.PIN);
            assertThat(pendingCardAuthorizationCache.take("payment-pin")).isPresent();
        }
    }

    @Nested
    @DisplayName("confirmFromGatewayCallback()")
    class GatewayCallback {

        @Test
        @DisplayName("confirme un paiement PENDING et déclenche les mêmes effets qu'un succès synchrone")
        void shouldConfirmPendingPaymentAndTriggerBookingConfirmation() {
            Payment pendingPayment = new Payment(BOOKING_ID, PRICE, PaymentMethod.MOBILE_MONEY, "6512", false, "CM", "XAF");
            pendingPayment.markPending("flw-ref-005");
            when(paymentRepository.findById("payment-1")).thenReturn(Optional.of(pendingPayment));
            when(bookingService.getSummary(BOOKING_ID)).thenReturn(fullPaymentBooking());

            ChargeResult confirmedResult = new ChargeResult(
                    ChargeStatus.SUCCEEDED, "flw-ref-005", "6512", null, null);

            paymentService.confirmFromGatewayCallback("payment-1", confirmedResult);

            assertThat(pendingPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
            verify(bookingService).markPaidAndConfirm(BOOKING_ID, "flw-ref-005", "6512");

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue()).isInstanceOf(BookingFullyPaidEvent.class);
        }

        @Test
        @DisplayName("ignore un callback si le paiement n'est plus PENDING (idempotence)")
        void shouldIgnoreCallbackIfAlreadyTerminal() {
            Payment alreadySucceeded = new Payment(BOOKING_ID, PRICE, PaymentMethod.CARD, "4242", false, "CM", "XAF");
            alreadySucceeded.markSucceeded("flw-ref-006");
            when(paymentRepository.findById("payment-2")).thenReturn(Optional.of(alreadySucceeded));

            ChargeResult duplicateCallback = new ChargeResult(
                    ChargeStatus.SUCCEEDED, "flw-ref-006", "4242", null, null);

            paymentService.confirmFromGatewayCallback("payment-2", duplicateCallback);

            verifyNoInteractions(bookingService, eventPublisher);
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("lève NotFoundException si le payment n'existe pas")
        void shouldThrowWhenPaymentNotFound() {
            when(paymentRepository.findById("unknown")).thenReturn(Optional.empty());

            ChargeResult result = new ChargeResult(ChargeStatus.SUCCEEDED, "ref", null, null, null);

            assertThatThrownBy(() -> paymentService.confirmFromGatewayCallback("unknown", result))
                    .isInstanceOf(NotFoundException.class);

            verifyNoInteractions(bookingService, eventPublisher);
        }
    }
    @Test
    @DisplayName("marque le paiement FAILED lors d'un callback d'échec (ex: timeout OTP Mobile Money)")
    void shouldMarkPaymentFailedOnFailedCallback() {
        Payment pendingPayment = new Payment(BOOKING_ID, PRICE, PaymentMethod.MOBILE_MONEY, "6512", false, "CM", "XAF");
        pendingPayment.markPending("flw-ref-007");
        when(paymentRepository.findById("payment-3")).thenReturn(Optional.of(pendingPayment));
        when(bookingService.getSummary(BOOKING_ID)).thenReturn(fullPaymentBooking());

        ChargeResult failedResult = new ChargeResult(
                ChargeStatus.FAILED, "flw-ref-007", "6512", "Délai de validation dépassé", null);

        paymentService.confirmFromGatewayCallback("payment-3", failedResult);

        assertThat(pendingPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(pendingPayment.getFailureReason()).isEqualTo("Délai de validation dépassé");

        // Aucun événement de succès ne doit être émis et la réservation reste inchangée, mais un
        // PaymentFailedEvent part bien (voir usernotification, qui prévient le client concerné)
        verify(bookingService, never()).markPaidAndConfirm(anyString(), anyString(), anyString());
        verify(bookingService, never()).markDepositPaid(anyString());
        verify(eventPublisher).publishEvent(
                new PaymentFailedEvent(BOOKING_ID, pendingPayment.getId(), "Délai de validation dépassé"));
        verify(paymentRepository).save(pendingPayment);
    }
}