ALTER TABLE payments
    ADD COLUMN country_code VARCHAR(2);
ALTER TABLE payments
    ADD COLUMN country_currency VARCHAR(3);