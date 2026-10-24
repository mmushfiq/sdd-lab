CREATE SEQUENCE room_type_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE room_type
(
    id           BIGINT DEFAULT NEXT VALUE FOR room_type_seq PRIMARY KEY,
    name         VARCHAR(50)    NOT NULL UNIQUE,
    description  VARCHAR(500),
    max_guests   INTEGER        NOT NULL CHECK (max_guests BETWEEN 1 AND 10),
    nightly_rate DECIMAL(10, 2) NOT NULL CHECK (nightly_rate BETWEEN 0 AND 99999999.99),
    active       BOOLEAN        NOT NULL
);
