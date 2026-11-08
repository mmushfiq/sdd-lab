-- 003 seasonal room rates (research R1, R2)
create sequence seasonal_rate_seq start with 1;

create table seasonal_rate
(
    id           bigint primary key,
    room_type_id bigint         not null references room_type (id) on delete cascade,
    name         varchar(50)    not null,
    start_date   date           not null,
    -- last night covered, inclusive
    end_date     date           not null,
    nightly_rate decimal(10, 2) not null check (nightly_rate > 0),
    check (end_date >= start_date)
);

create index seasonal_rate_room_type_dates on seasonal_rate (room_type_id, start_date);

-- One row per night of a stay, rate frozen at booking (FR-006)
create table reservation_night
(
    reservation_id bigint         not null references reservation (id),
    night_date     date           not null,
    nightly_rate   decimal(10, 2) not null check (nightly_rate > 0),
    primary key (reservation_id, night_date)
);

-- Existing reservations keep their price: every night at the old nightly rate (FR-008); stays have at most 30 nights
insert into reservation_night (reservation_id, night_date, nightly_rate)
select r.id, dateadd(day, n.x, r.arrival_date), r.nightly_rate
from reservation r
         join system_range(0, 29) n on n.x < datediff(day, r.arrival_date, r.departure_date);

-- New bookings no longer write it; V6 drops it
alter table reservation alter column nightly_rate set null;
