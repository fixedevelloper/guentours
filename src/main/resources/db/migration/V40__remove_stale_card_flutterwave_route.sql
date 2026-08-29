-- A global CARD -> FLUTTERWAVE admin route predates the Stripe switch (PaymentProviderRoutingService.
-- DEFAULT_PROVIDER is now STRIPE) and was overriding it, so every CARD/GOOGLE_PAY/APPLE_PAY/PAYPAL
-- charge still resolved to Flutterwave - which needs the raw card fields (cardNumber/expiry/cvv)
-- the frontend no longer sends now that Stripe collects them itself, crashing with a
-- NullPointerException on ChargeRequest.expiry(). Deleted rather than deactivated: an inactive
-- route still rejects the charge outright instead of falling through to the new default (see
-- PaymentProviderRoutingService.requireActive) - removing it entirely lets CARD fall through to
-- STRIPE, the intended behavior. Re-add via the admin UI if Flutterwave-for-CARD is ever wanted
-- again deliberately.
DELETE FROM payment_provider_routes WHERE country_code IS NULL AND payment_method = 'CARD' AND provider_name = 'FLUTTERWAVE';
