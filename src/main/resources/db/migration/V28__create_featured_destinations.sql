-- Backs the homepage "popular destinations" section: rows are auto-suggested from real flight
-- booking volume (see FeaturedDestinationService.refreshFromBookings) but fully admin-editable
-- afterward (display order, image, active flag) via /api/admin/destinations.
create table featured_destinations (
    id varchar(255) not null,
    city_name varchar(150) not null,
    country_name varchar(150) not null,
    destination_code varchar(10),
    image_url varchar(1024),
    display_order int not null default 0,
    active bit not null default true,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id)
) engine=InnoDB;

create unique index uk_featured_destinations_code on featured_destinations (destination_code);
create index idx_featured_destinations_active_order on featured_destinations (active, display_order);
