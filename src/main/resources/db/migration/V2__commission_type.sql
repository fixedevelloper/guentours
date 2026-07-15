-- Distinguishes the fixed booking fee (added at checkout) from the non-refundable PAY_LATER
-- reservation fee (recorded when the customer pays it). Existing rows are all booking fees.
alter table commission_wallet_entries
    add column commission_type enum ('BOOKING_FEE', 'RESERVATION_FEE') not null default 'BOOKING_FEE';
