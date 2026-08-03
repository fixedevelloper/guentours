-- The number of rooms a hotel booking was checked out for gets multiplied into the total price
-- at checkout time (see BookingService.buildPendingHotelBooking) but was never itself persisted,
-- so by the time the async provider hold runs it had no way to ask Travelport for more than one
-- room's worth of availability/price, or to actually reserve more than one room. Always 1 for
-- non-hotel bookings.
alter table bookings add column room_quantity int not null default 1;
