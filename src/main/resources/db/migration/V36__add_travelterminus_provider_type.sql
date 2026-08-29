-- ProviderType gained TRAVELTERMINUS (new flight provider); the enum columns storing it must be
-- widened or every booking/commission insert for that provider fails with "Data truncated for
-- column 'provider_type'".
ALTER TABLE bookings MODIFY COLUMN provider_type
    ENUM('SABRE','TRAVELOPRO','TRAVELPORT','TRAVELTERMINUS','DIRECT') NOT NULL;

-- commission_wallet_entries was never widened for 'DIRECT' either (pre-existing gap, unrelated to
-- Travel Terminus) - fixed here too since it sits in the same booking-commission path.
ALTER TABLE commission_wallet_entries MODIFY COLUMN provider_type
    ENUM('SABRE','TRAVELOPRO','TRAVELPORT','TRAVELTERMINUS','DIRECT') NOT NULL;
