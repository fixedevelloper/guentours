package com.guentours.payment;

import com.guentours.booking.BookingService;
import com.guentours.booking.BookingStatus;
import com.guentours.booking.BookingSummary;
import com.guentours.booking.PaymentPlan;
import com.guentours.payment.gateway.ChargeRequest;
import com.guentours.payment.gateway.ChargeResult;
import com.guentours.payment.gateway.PaymentGateway;
import com.guentours.shared.exception.BusinessException;
import com.guentours.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final BookingService bookingService;

    public PaymentService(PaymentRepository paymentRepository, PaymentGateway paymentGateway,
                           BookingService bookingService) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.bookingService = bookingService;
    }

    @Transactional
    public Payment pay(PaymentRequest request) {
        BookingSummary booking = bookingService.getSummary(request.bookingId());
        if (booking.status() != BookingStatus.PENDING_PAYMENT && booking.status() != BookingStatus.DEPOSIT_PAID) {
            throw new BusinessException("Booking " + booking.id() + " is not awaiting payment");
        }
        boolean isDepositPayment = booking.status() == BookingStatus.PENDING_PAYMENT
                && booking.paymentPlan() == PaymentPlan.PAY_LATER;

        validatePaymentMethodFields(request);
        var amountDue = booking.amountDue();

        ChargeRequest chargeRequest = new ChargeRequest(amountDue, request.paymentMethod(), request.cardNumber(),
                request.cardHolderName(), request.expiry(), request.cvv(), request.mobileNumber());
        Payment payment = new Payment(booking.id(), amountDue, request.paymentMethod(), chargeRequest.payerReferenceLast4());

        ChargeResult result = paymentGateway.charge(chargeRequest);
        if (result.success()) {
            payment.markSucceeded(result.gatewayReference());
            paymentRepository.save(payment);
            if (isDepositPayment) {
                bookingService.markDepositPaid(booking.id());
            } else {
                bookingService.markPaidAndConfirm(booking.id(), result.gatewayReference(), chargeRequest.payerReferenceLast4());
            }
        } else {
            payment.markFailed(result.failureReason());
            paymentRepository.save(payment);
        }
        return payment;
    }

    public Payment getById(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + paymentId));
    }

    private void validatePaymentMethodFields(PaymentRequest request) {
        if (request.paymentMethod() == PaymentMethod.CARD) {
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
        } else {
            if (isBlank(request.mobileNumber()) || !request.mobileNumber().matches("\\+?\\d{8,15}")) {
                throw new BusinessException("mobileNumber must be a valid phone number");
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
