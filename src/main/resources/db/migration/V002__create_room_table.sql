CREATE SEQUENCE room_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE room
(
    id              BIGINT DEFAULT NEXT VALUE FOR room_seq PRIMARY KEY,
    room_number     VARCHAR(10) NOT NULL UNIQUE,
    room_type_id    BIGINT      NOT NULL,
    cleaning_status VARCHAR(10) NOT NULL CHECK (cleaning_status IN ('CLEAN', 'DIRTY')),
    active          BOOLEAN     NOT NULL,
    CONSTRAINT room_room_type_fk FOREIGN KEY (room_type_id) REFERENCES room_type (id),
    -- Target of reservation_room_fk, which ensures an assigned room belongs to the reserved room type
    CONSTRAINT room_id_room_type_uk UNIQUE (id, room_type_id)
);
