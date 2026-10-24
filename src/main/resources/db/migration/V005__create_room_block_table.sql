CREATE SEQUENCE room_block_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE room_block
(
    id         BIGINT DEFAULT NEXT VALUE FOR room_block_seq PRIMARY KEY,
    room_id    BIGINT NOT NULL,
    start_date DATE   NOT NULL,
    end_date   DATE   NOT NULL,
    reason     VARCHAR(200),
    CONSTRAINT room_block_room_fk FOREIGN KEY (room_id) REFERENCES room (id),
    CONSTRAINT room_block_dates_ck CHECK (end_date >= start_date)
);
