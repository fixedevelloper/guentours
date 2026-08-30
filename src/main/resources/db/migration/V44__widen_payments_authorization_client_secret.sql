-- VARCHAR(255) (V37) was sized for a PaymentIntent client_secret ("pi_..._secret_..."), which fits
-- comfortably. A Checkout Session client_secret ("cs_..._secret_...", used since the Embedded
-- Checkout migration) runs noticeably longer and overflows it, failing the payment with a raw
-- "Data too long for column" DataIntegrityViolationException.
ALTER TABLE payments MODIFY COLUMN authorization_client_secret VARCHAR(512);
