-- 003 research R2: night rows (V5) replaced the single nightly rate of a reservation; no code reads it any more
alter table reservation drop column nightly_rate;
