-- Admin-only diagnostic detail: the provider's machine-readable error code (e.g. Travel Terminus's
-- insufficient_funds/unauthorized/provider_down/invalid_request), captured alongside the sanitized,
-- customer-safe failure_reason column (see Booking#markFailed / ProviderException#code) so an admin
-- can identify the real cause without digging through logs.
ALTER TABLE bookings ADD COLUMN provider_error_code VARCHAR(64);
