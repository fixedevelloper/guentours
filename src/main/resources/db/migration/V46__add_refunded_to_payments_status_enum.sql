-- payments.status is a real MySQL ENUM (V1__init.sql), so adding PaymentStatus.REFUNDED
-- (PaymentService#refundForBooking) needs a schema change, not just a Java enum change - writing
-- "REFUNDED" against the old enum('PENDING','PENDING_AUTHORIZATION','SUCCEEDED','FAILED') fails
-- with "Data truncated for column 'status'".
ALTER TABLE payments MODIFY COLUMN status
    ENUM('PENDING','PENDING_AUTHORIZATION','SUCCEEDED','FAILED','REFUNDED') NOT NULL;
