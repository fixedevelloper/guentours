-- Tracks whether the (already captured) payment for a booking has since been refunded by an admin
-- (see PaymentService#refundForBooking) - kept separate from payment_captured (V42), which records
-- history ("money was taken") and must stay true even after a refund.
ALTER TABLE bookings ADD COLUMN payment_refunded BIT NOT NULL DEFAULT FALSE;
