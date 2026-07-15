/**
 * Commission module: records the fixed booking fee earned on every flight/hotel booking into
 * a wallet ledger. The fee is added on top of the provider's own price (see
 * {@code CommissionPolicy} in the shared kernel) rather than deducted from it, so this module
 * only tracks GuenTours' own revenue - it never touches what a provider is paid.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"booking", "provider", "shared"}
)
package com.guentours.commission;
