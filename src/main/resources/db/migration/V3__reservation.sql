create sequence reservation_seq start with 1;

create table reservation
(
    id                 bigint primary key,
    reservation_number char(8)        not null unique,
    room_id            bigint         not null references room (id),
    arrival_date       date           not null,
    departure_date     date           not null,
    number_of_guests   int            not null check (number_of_guests >= 1),
    guest_name         varchar(100)   not null,
    -- NULL only after anonymization (FR-026)
    guest_email        varchar(254),
    guest_phone        varchar(30),
    nightly_rate       decimal(10, 2) not null,
    total_price        decimal(10, 2) not null,
    status             varchar(12)    not null check (status in ('BOOKED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')),
    source             varchar(10)    not null check (source in ('ONLINE', 'RECEPTION')),
    cancelled_at       timestamp,
    anonymized         boolean default false not null,
    created_at         timestamp      not null,
    check (departure_date > arrival_date)
);

create index reservation_room_dates on reservation (room_id, arrival_date, departure_date);
