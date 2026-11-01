ALTER TABLE reservation DROP CONSTRAINT reservation_status_ck;

ALTER TABLE reservation
    ADD CONSTRAINT reservation_status_ck CHECK (status IN ('BOOKED', 'CANCELLED', 'CHECKED_IN', 'CHECKED_OUT'));
