-- PaymentProviderRoutingService.DEFAULT_PROVIDER just changed from FLUTTERWAVE to STRIPE (Google
-- Pay/Apple Pay support). Stripe doesn't process African mobile money, so without an explicit
-- global route MOBILE_MONEY charges would silently start resolving to STRIPE and fail outright.
-- Guarded with WHERE NOT EXISTS so this is a no-op if an admin already configured this route by
-- hand before this migration ran (findByCountryCodeIsNullAndPaymentMethod expects at most one row).
INSERT INTO payment_provider_routes (id, country_code, payment_method, provider_name, active, created_at, updated_at)
SELECT UUID(), NULL, 'MOBILE_MONEY', 'FLUTTERWAVE', 1, NOW(6), NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM payment_provider_routes WHERE country_code IS NULL AND payment_method = 'MOBILE_MONEY'
);
