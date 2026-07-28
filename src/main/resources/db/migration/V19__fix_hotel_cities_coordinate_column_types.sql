-- Some existing databases have hotel_cities.latitude/longitude as DECIMAL instead of the
-- DOUBLE (float(53)) type the HotelCity entity and V1__init.sql expect, which makes Hibernate's
-- schema validation fail at startup with "wrong column type ... found [decimal], but expecting
-- [float(53)]". Align the physical column type so ddl-auto=validate passes.
ALTER TABLE hotel_cities
    MODIFY COLUMN latitude DOUBLE NOT NULL,
    MODIFY COLUMN longitude DOUBLE NOT NULL;
