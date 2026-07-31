-- Cards that require Flutterwave PIN/AVS/3DS authorization used to get stuck forever: the payment
-- was simply marked PENDING like an ordinary async (mobile money/PayPal) wait, with no way to ever
-- submit the follow-up PIN. PENDING_AUTHORIZATION distinguishes that synchronous-challenge wait, and
-- the two new columns record which challenge is outstanding (never the card PAN/CVV itself - those
-- stay in memory only, see PendingCardAuthorizationCache).
ALTER TABLE payments MODIFY COLUMN status
    ENUM ('PENDING', 'PENDING_AUTHORIZATION', 'SUCCEEDED', 'FAILED') NOT NULL;

ALTER TABLE payments ADD COLUMN authorization_type VARCHAR(20);
ALTER TABLE payments ADD COLUMN authorization_redirect_url VARCHAR(2048);
