-- Test data only: the test data files insert reservations without reservation nights. Fill them the same way as V008:
-- every night gets total price / number of nights, rounded down to the cent; the last night gets the remainder.
-- Runs after all test data files.

INSERT INTO reservation_night (reservation_id, night_date, night_price)
WITH RECURSIVE nights (reservation_id, night_date, departure_date, night_count, total_price) AS (
    SELECT id, arrival_date, departure_date, DATEDIFF(DAY, arrival_date, departure_date), total_price
    FROM reservation r
    WHERE NOT EXISTS (SELECT 1 FROM reservation_night n WHERE n.reservation_id = r.id)
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
