-- Travelport's hotel reservation cancellation call (PUT /hotel/book/reservations/{reservationIdentifier}
-- /canceloffer?supplierLocator=...&offerID=...) needs a supplier-specific locator distinct from the
-- GDS-level confirmation already stored in provider_confirmation_number - captured once at hold time
-- (see TravelportClient.callHotelReservationApi) since it can't be recovered later from the short-lived
-- offer cache. Null for non-Travelport hotel bookings and for every non-hotel offer type.
alter table bookings add column hotel_supplier_locator varchar(255);
