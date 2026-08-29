-- Extras (baggage/meal/seat/insurance) picked at the new "additional options" checkout step -
-- FLIGHT only today. One row per selected extra; traveler_index is the traveler's position in
-- booking_travelers (null for a booking-level extra like INSURANCE). provider_token is the opaque
-- value the originating provider adapter needs back unmodified to apply the selection at hold
-- time (null for INSURANCE, which is never sent upstream).
CREATE TABLE booking_extras (
    booking_id varchar(255) NOT NULL,
    extra_type varchar(20) NOT NULL,
    traveler_index integer,
    segment_id varchar(255),
    code varchar(255),
    label varchar(255),
    amount decimal(38,2),
    currency varchar(255),
    provider_token tinytext
) engine=InnoDB;

ALTER TABLE booking_extras ADD CONSTRAINT fk_booking_extras_booking FOREIGN KEY (booking_id) REFERENCES bookings (id);

CREATE INDEX idx_booking_extras_booking_id ON booking_extras (booking_id);
