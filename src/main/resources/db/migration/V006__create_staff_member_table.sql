CREATE SEQUENCE staff_member_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE staff_member
(
    id            BIGINT DEFAULT NEXT VALUE FOR staff_member_seq PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('RECEPTIONIST', 'MANAGER', 'HOUSEKEEPER')),
    active        BOOLEAN      NOT NULL
);
