-- CR-002: Seasons with their own nightly rate per room type (UC-025), and the price of every night of a reservation.

CREATE SEQUENCE season_seq START WITH 1 INCREMENT BY 1 CACHE 50;

-- Seasons must not overlap (UC-025 BR-002); the application checks this before saving.
CREATE TABLE season
(
    id          BIGINT DEFAULT NEXT VALUE FOR season_seq PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    first_night DATE        NOT NULL,
    last_night  DATE        NOT NULL,
    CONSTRAINT season_nights_ck CHECK (last_night >= first_night)
);

CREATE INDEX season_nights_idx ON season (first_night, last_night);

CREATE TABLE season_rate
(
    season_id    BIGINT         NOT NULL,
    room_type_id BIGINT         NOT NULL,
    nightly_rate DECIMAL(10, 2) NOT NULL CHECK (nightly_rate BETWEEN 0 AND 99999999.99),
    PRIMARY KEY (season_id, room_type_id),
    CONSTRAINT season_rate_season_fk FOREIGN KEY (season_id) REFERENCES season (id) ON DELETE CASCADE,
    CONSTRAINT season_rate_room_type_fk FOREIGN KEY (room_type_id) REFERENCES room_type (id)
);

CREATE TABLE reservation_night
(
    reservation_id BIGINT         NOT NULL,
    night_date     DATE           NOT NULL,
    night_price    DECIMAL(10, 2) NOT NULL CHECK (night_price BETWEEN 0 AND 99999999.99),
    PRIMARY KEY (reservation_id, night_date),
    CONSTRAINT reservation_night_reservation_fk FOREIGN KEY (reservation_id) REFERENCES reservation (id) ON DELETE CASCADE
);

-- Existing reservations: every night gets total price / number of nights, rounded down to the cent; the last night
-- gets the remainder, so the sum of the night prices equals the total price.
INSERT INTO reservation_night (reservation_id, night_date, night_price)
WITH RECURSIVE nights (reservation_id, night_date, departure_date, night_count, total_price) AS (
    SELECT id, arrival_date, departure_date, DATEDIFF(DAY, arrival_date, departure_date), total_price
    FROM reservation
    UNION ALL
    SELECT reservation_id, DATEADD(DAY, 1, night_date), departure_date, night_count, total_price
    FROM nights
    WHERE DATEADD(DAY, 1, night_date) < departure_date
)
SELECT reservation_id,
       night_date,
       CASE
           WHEN DATEADD(DAY, 1, night_date) = departure_date
               THEN total_price - (night_count - 1)
                   * TRUNC(CAST(total_price AS DECIMAL(20, 10)) / night_count, 2)
           ELSE TRUNC(CAST(total_price AS DECIMAL(20, 10)) / night_count, 2)
           END
FROM nights;
