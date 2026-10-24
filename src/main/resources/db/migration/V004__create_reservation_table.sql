CREATE SEQUENCE reservation_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE reservation
(
    id               BIGINT DEFAULT NEXT VALUE FOR reservation_seq PRIMARY KEY,
    reservation_code VARCHAR(20)    NOT NULL UNIQUE,
    guest_id         BIGINT         NOT NULL,
    room_type_id     BIGINT         NOT NULL,
    room_id          BIGINT,
    arrival_date     DATE           NOT NULL,
    departure_date   DATE           NOT NULL,
    number_of_guests INTEGER        NOT NULL CHECK (number_of_guests BETWEEN 1 AND 10),
    total_price      DECIMAL(10, 2) NOT NULL CHECK (total_price BETWEEN 0 AND 99999999.99),
    status           VARCHAR(20)    NOT NULL
        CHECK (status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED', 'NO_SHOW')),
    channel          VARCHAR(20)    NOT NULL CHECK (channel IN ('ONLINE', 'RECEPTION')),
    created_at       TIMESTAMP      NOT NULL,
    checked_in_at    TIMESTAMP,
    checked_out_at   TIMESTAMP,
    cancelled_at     TIMESTAMP,
    CONSTRAINT reservation_guest_fk FOREIGN KEY (guest_id) REFERENCES guest (id),
    CONSTRAINT reservation_room_type_fk FOREIGN KEY (room_type_id) REFERENCES room_type (id),
    -- The assigned room must belong to the reserved room type
    CONSTRAINT reservation_room_fk FOREIGN KEY (room_id, room_type_id) REFERENCES room (id, room_type_id),
    CONSTRAINT reservation_dates_ck CHECK (departure_date > arrival_date),
    -- A room is assigned and checked_in_at is set exactly when the status is Checked In or Checked Out
    CONSTRAINT reservation_room_assigned_ck CHECK (
        (status IN ('CHECKED_IN', 'CHECKED_OUT') AND room_id IS NOT NULL AND checked_in_at IS NOT NULL)
            OR (status NOT IN ('CHECKED_IN', 'CHECKED_OUT') AND room_id IS NULL AND checked_in_at IS NULL)),
    CONSTRAINT reservation_checked_out_ck CHECK ((status = 'CHECKED_OUT') = (checked_out_at IS NOT NULL)),
    CONSTRAINT reservation_cancelled_ck CHECK ((status = 'CANCELLED') = (cancelled_at IS NOT NULL))
);

CREATE INDEX reservation_room_type_dates_idx ON reservation (room_type_id, arrival_date, departure_date);
