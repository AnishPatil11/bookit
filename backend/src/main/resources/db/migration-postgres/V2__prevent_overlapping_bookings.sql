-- PostgreSQL only: the database itself refuses two CONFIRMED bookings that overlap on the same table.
-- This is the last line of defence behind the application-level locking in BookingService.
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE bookings
    ADD CONSTRAINT bookings_no_overlap
    EXCLUDE USING gist (
        dining_table_id WITH =,
        tsrange(starts_at, ends_at) WITH &&
    ) WHERE (status = 'CONFIRMED');
