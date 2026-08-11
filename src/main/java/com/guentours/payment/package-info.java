/**
 * Payment module: charges the authoritative booking price (never a client-supplied
 * amount) through a pluggable {@code PaymentGateway}, then triggers provider
 * confirmation on success. Card numbers/CVVs are never persisted - only the last
 * four digits and the gateway's own reference are kept for support/audit purposes.
 *
 * <p>{@code type = OPEN}: {@code Payment} (payment.domain), {@code PaymentService}
 * (payment.service) and the payment lifecycle events (payment.events) are read directly by
 * commission, usernotification and reseller, which all need to react to or report on payment
 * outcomes - the actual PSP integration (gateway, web) is what stays sensitive, and nothing
 * outside this module reaches into those.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"booking", "shared"},
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.guentours.payment;
