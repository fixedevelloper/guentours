/**
 * Newsletter module: public email signup from marketing pages (flight/hotel search results).
 * Subscription storage is deliberately kept independent of {@code user} - a guest without an
 * account can subscribe - and confirmation email is sent via a published event consumed by the
 * {@code notification} module rather than a direct call, since its {@code EmailService} is
 * package-private.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"shared"}
)
package com.guentours.newsletter;
