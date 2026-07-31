-- Lets a failed provider hold be retried without re-collecting the guest's contact details:
-- the original search offer id(s) and phone number are kept on the booking itself.
alter table bookings add column search_offer_id text null;
alter table bookings add column contact_phone varchar(30) null;
