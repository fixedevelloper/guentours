-- Booking#markPaid now also flips paymentCaptured=true, and never unsets it - it's the only
-- reliable way to tell a booking that failed after payment was actually charged (confirmWithProvider
-- failure - see BookingService) apart from one that never got charged at all, once both are just
-- FAILED. The frontend uses this to hide "search again" (which risks a second charge) in favor of a
-- support-contact message for the former.
-- Defaults existing rows to FALSE, then backfills the ones that actually have a SUCCEEDED payment on
-- record, so already-affected bookings get the correct UI immediately rather than waiting for a new
-- payment attempt.
ALTER TABLE bookings ADD COLUMN payment_captured BIT NOT NULL DEFAULT FALSE;

UPDATE bookings b
JOIN payments p ON p.booking_id = b.id
SET b.payment_captured = TRUE
WHERE p.status = 'SUCCEEDED';
