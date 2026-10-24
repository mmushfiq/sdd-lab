CREATE SEQUENCE guest_seq START WITH 1 INCREMENT BY 1 CACHE 50;

-- Email is stored in lower case, so the unique constraint is case-insensitive.
CREATE TABLE guest
(
    id         BIGINT DEFAULT NEXT VALUE FOR guest_seq PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE CHECK (email = LOWER(email)),
    phone      VARCHAR(30)  NOT NULL
);
