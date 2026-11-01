-- Every reservation keeps its rates per night in reservation_night (V7); the single rate is no longer used.
ALTER TABLE reservation DROP COLUMN nightly_rate;
