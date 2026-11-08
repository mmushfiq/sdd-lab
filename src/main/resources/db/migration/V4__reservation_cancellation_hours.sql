-- 002: hours before check-in (15:00 on the arrival date) until which a guest may cancel online.
-- Existing reservations keep the 001 rule (15 h = until 00:00 on the arrival date); new ones get 48 h.
alter table reservation add column cancellation_hours_before int default 15 not null;
alter table reservation alter column cancellation_hours_before set default 48;
