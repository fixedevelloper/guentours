-- Stripe's PaymentIntent flow needs a client_secret handed back to the frontend so it can call
-- stripe.confirmPayment itself (CLIENT_ACTION authorization type - see AuthorizationChallenge).
-- authorization_type stays VARCHAR(20) (added in V23, not a native ENUM) so the new value needs no
-- column change there, unlike a real MySQL ENUM column (see V36's provider_type widening).
ALTER TABLE payments ADD COLUMN authorization_client_secret VARCHAR(255);
