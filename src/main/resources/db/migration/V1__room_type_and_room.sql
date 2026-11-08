create sequence room_type_seq start with 1;

create table room_type
(
    id           bigint primary key,
    name         varchar(50)    not null unique,
    capacity     int            not null check (capacity >= 1),
    nightly_rate decimal(10, 2) not null check (nightly_rate > 0)
);

create sequence room_seq start with 1;

create table room
(
    id              bigint primary key,
    room_number     varchar(10) not null unique,
    room_type_id    bigint      not null references room_type (id),
    cleaning_status varchar(10) default 'CLEAN' not null check (cleaning_status in ('CLEAN', 'DIRTY'))
);
