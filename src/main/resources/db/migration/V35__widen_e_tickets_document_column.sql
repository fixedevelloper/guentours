-- e_tickets.document was created as `tinytext` (255-byte MySQL max) in the V1 baseline - the
-- actual rendered ticket text (see ETicketService.renderDocument) has always exceeded that once
-- real booking/ticket/confirmation values are filled in, silently failing every e-ticket insert
-- in MySQL (never caught by tests: H2's ddl-auto generates an unbounded column from the same
-- @Lob annotation, with no such limit). `text` holds up to 64KB, comfortably enough.
alter table e_tickets modify column document text;
