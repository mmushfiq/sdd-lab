create sequence staff_user_seq start with 1;

create table staff_user
(
    id            bigint primary key,
    username      varchar(50)  not null unique,
    password_hash varchar(100) not null,
    role          varchar(20)  not null check (role in ('RECEPTIONIST', 'MANAGER', 'HOUSEKEEPING')),
    active        boolean default true not null
);

-- Initial Manager (research R2): password "manager", to be reset by the Manager after installation
insert into staff_user (id, username, password_hash, role, active)
values (next value for staff_user_seq, 'manager',
        '{bcrypt}$2a$10$atSg1mNn7vWD4UINXu0Jq.YExjJ0ttAVNZETmThxpmdAskXlONbu2', 'MANAGER', true);
