-- Test data only: reservations that did not set a deadline get the current rule (UC-003 BR-001):
-- 48 hours before the check-in time (14:00) on the arrival date. Runs after all test data files.

UPDATE reservation
SET online_cancellation_deadline = DATEADD(HOUR, 14 - 48, CAST(arrival_date AS TIMESTAMP))
WHERE online_cancellation_deadline IS NULL;

ALTER TABLE reservation ALTER COLUMN online_cancellation_deadline SET NOT NULL;
