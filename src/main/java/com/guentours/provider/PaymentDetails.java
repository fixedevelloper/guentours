package com.guentours.provider;

import com.guentours.shared.Money;

/**
 * Proof of payment capture passed to the provider when issuing tickets/finalizing a
 * hotel booking - GDS ticketing APIs generally require evidence the fare was paid for
 * before they will convert a hold into an issued e-ticket.
 */
public record PaymentDetails(String transactionReference, Money amount, String cardLast4) {
}
