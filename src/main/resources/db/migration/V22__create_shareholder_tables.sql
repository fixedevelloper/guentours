-- Shareholders are a fixed company equity split applied to every commission GuenTours earns
-- (never at search time, only once a booking's commission is actually recorded) - independent of
-- any user/reseller/partner account.
create table shareholders (
    id varchar(255) not null,
    name varchar(255) not null,
    percentage decimal(5,2) not null,
    active bit not null,
    created_at datetime(6) not null,
    primary key (id)
) engine=InnoDB;

-- One shareholder's cut of a single commission_wallet_entries row, snapshotting the shareholder's
-- name and percentage as they were at that moment so a later change never rewrites history.
create table shareholder_commission_entries (
    id varchar(255) not null,
    commission_wallet_entry_id varchar(255) not null,
    shareholder_id varchar(255) not null,
    shareholder_name varchar(255) not null,
    percentage_applied decimal(5,2) not null,
    amount decimal(38,2),
    currency varchar(255),
    created_at datetime(6) not null,
    primary key (id)
) engine=InnoDB;

create index idx_shareholder_commission_entries_shareholder on shareholder_commission_entries (shareholder_id);
create index idx_shareholder_commission_entries_wallet_entry on shareholder_commission_entries (commission_wallet_entry_id);
