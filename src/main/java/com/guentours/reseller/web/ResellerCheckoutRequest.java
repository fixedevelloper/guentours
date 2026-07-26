package com.guentours.reseller.web;

import com.guentours.booking.web.CheckoutRequest;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ResellerCheckoutRequest(
        @NotBlank CheckoutRequest checkoutRequest,
        @NotBlank BigDecimal customAmount
) {

}