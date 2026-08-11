-- Public MinIO URL of the branded PDF version of the ticket (company logo, plus the reseller's
-- when the booking went through one). Nullable: PDF generation failures never block ticket
-- creation - the existing plain-text `document` column stays the fallback.
alter table e_tickets add column pdf_url varchar(512);
