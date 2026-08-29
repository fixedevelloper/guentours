-- Booking#canRetryHold used to consider every FAILED, unconfirmed hold retryable - including
-- OfferExpiredException failures, which can never succeed on retry since retrying resends the
-- exact same now-dead offer id (see BookingService.completeHold/completeMultiCityHold). That sent
-- payers into a "Retry" loop that fails identically every time instead of prompting a fresh search.
-- Defaults existing rows to retryable=true (unchanged behavior for historical data - there's no
-- reliable way to tell past offer-expiry failures apart from other failures after the fact).
ALTER TABLE bookings ADD COLUMN retryable BIT NOT NULL DEFAULT TRUE;
