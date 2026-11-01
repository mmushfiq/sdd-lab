-- Online cancellation deadline = check-in (14:00 on the arrival date) minus these hours.
-- Existing reservations keep the old rule (00:00 on the arrival date = 14 hours), new ones get 48 hours.
ALTER TABLE reservation
    ADD COLUMN cancellation_hours_before INT DEFAULT 14 NOT NULL CHECK (cancellation_hours_before >= 0);
ALTER TABLE reservation
    ALTER COLUMN cancellation_hours_before SET DEFAULT 48;
