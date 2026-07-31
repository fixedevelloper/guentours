-- Lets admins enable/disable which payment gateway handles a given payment method per country
-- (e.g. Flutterwave for CARD in Cameroon, a different operator for MOBILE_MONEY), looked up by
-- PaymentProviderRoutingService before every charge. country_code null means the rule is the
-- default for every country not covered by a more specific rule for that same payment_method.
create table payment_provider_routes (
    id varchar(255) not null,
    country_code varchar(2),
    payment_method enum ('CARD', 'GOOGLE_PAY', 'APPLE_PAY', 'MOBILE_MONEY', 'PAYPAL') not null,
    provider_name varchar(50) not null,
    active bit not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id)
) engine=InnoDB;

create index idx_payment_provider_routes_country_method on payment_provider_routes (country_code, payment_method);
