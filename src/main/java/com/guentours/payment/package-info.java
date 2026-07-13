/**
 * Payment module: charges the authoritative booking price (never a client-supplied
 * amount) through a pluggable {@code PaymentGateway}, then triggers provider
 * confirmation on success. Card numbers/CVVs are never persisted - only the last
 * four digits and the gateway's own reference are kept for support/audit purposes.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"booking", "shared"}
)
package com.guentours.payment;
