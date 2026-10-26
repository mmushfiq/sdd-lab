-- Test data only: lets the test data files insert reservations without an online cancellation deadline.
-- V199 fills the missing deadlines and makes the column Not Null again.

ALTER TABLE reservation ALTER COLUMN online_cancellation_deadline SET NULL;
