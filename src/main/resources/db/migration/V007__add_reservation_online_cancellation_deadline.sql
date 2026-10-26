-- CR-001: The online cancellation deadline is stored per reservation (UC-003 BR-001).
-- Reservations recorded before the change keep the old rule: 24 hours before the check-in time (14:00)
-- on the arrival date. New reservations get 48 hours, set by the application.

ALTER TABLE reservation ADD COLUMN online_cancellation_deadline TIMESTAMP;

UPDATE reservation
SET online_cancellation_deadline = DATEADD(HOUR, 14 - 24, CAST(arrival_date AS TIMESTAMP));

ALTER TABLE reservation ALTER COLUMN online_cancellation_deadline SET NOT NULL;
