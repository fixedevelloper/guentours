-- Checkout now returns before the provider hold completes (see BookingService.checkout/
-- completeHold), so a booking can briefly sit in a new PENDING_HOLD status - add it to the
-- closed MySQL ENUM list, or every insert of a freshly-created booking fails with
-- "Data truncated for column 'status'".
alter table bookings modify column status
    enum ('CANCELLED','CONFIRMED','CONFIRMING','DEPOSIT_PAID','FAILED','PAID','PENDING_HOLD','PENDING_PAYMENT') not null;
