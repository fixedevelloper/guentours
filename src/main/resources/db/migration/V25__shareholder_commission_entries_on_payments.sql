-- Shareholder splits now trigger on every successful payment (deposit included), not on the
-- GuenTours commission fee anymore. The two mechanisms are independent: CommissionWalletEntry
-- keeps recording GuenTours' own revenue untouched, while this table now links to payments.
alter table shareholder_commission_entries drop index idx_shareholder_commission_entries_wallet_entry;
alter table shareholder_commission_entries change column commission_wallet_entry_id payment_id varchar(255) not null;
create index idx_shareholder_commission_entries_payment on shareholder_commission_entries (payment_id);
