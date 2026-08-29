-- event_publication.serialized_event was created as varchar(255) in the V1 baseline (Spring
-- Modulith's event publication registry, logging every published application event as JSON for
-- tracking/replay of incomplete listeners) - same class of bug as V35's e_tickets.document: any
-- event carrying more than a couple of small fields (BookingFullyPaidEvent, PaymentFailedEvent,
-- etc. with nested Money/ids/references) overflows 255 chars and fails the insert with "Data too
-- long for column 'serialized_event'", surfacing on the very first payment attempt of any kind.
-- Never caught by tests: H2's ddl-auto generates this table fresh with no such limit.
alter table event_publication modify column serialized_event text;
