-- Test data for UC-007 Search Reservations. Tests run with today = 2030-06-01 (Europe/Zurich).
-- The reservations do not reduce the availability that other tests search: the checked-in reservation is for the
-- inactive Suite (never offered), the others are in April 2031 or cancelled.

INSERT INTO guest (id, first_name, last_name, email, phone)
VALUES (1501, 'Sophie', 'Suchmann', 'sophie.suchmann@example.com', '+41 79 150 15 01'),
       (1502, 'Max', 'Vielbucher', 'max.vielbucher@example.com', '+41 79 150 15 02');

INSERT INTO reservation (id, reservation_code, guest_id, room_type_id, room_id, arrival_date, departure_date,
                         number_of_guests, total_price, status, channel, created_at, checked_in_at, checked_out_at,
                         cancelled_at)
VALUES
    -- Confirmed, made at the reception
    (1501, 'SEARCH0001', 1501, 1002, NULL, '2031-04-10', '2031-04-12', 2, 300.00, 'CONFIRMED', 'RECEPTION',
     '2030-05-01 10:00:00', NULL, NULL, NULL),
    -- Cancelled, made online
    (1502, 'SEARCH0002', 1501, 1001, NULL, '2031-04-05', '2031-04-06', 1, 100.00, 'CANCELLED', 'ONLINE',
     '2030-05-01 10:00:00', NULL, NULL, '2030-05-02 10:00:00'),
    -- Checked In to room 401 (Suite)
    (1503, 'SEARCH0003', 1501, 1004, 1006, '2030-05-31', '2030-06-02', 2, 800.00, 'CHECKED_IN', 'ONLINE',
     '2030-05-01 10:00:00', '2030-05-31 15:00:00', NULL, NULL);

-- 101 cancelled reservations of Max Vielbucher, one more than a search lists (UC-007 BR-003)
INSERT INTO reservation (id, reservation_code, guest_id, room_type_id, room_id, arrival_date, departure_date,
                         number_of_guests, total_price, status, channel, created_at, cancelled_at)
SELECT 1600 + x, 'MANY' || LPAD(CAST(x AS VARCHAR), 4, '0'), 1502, 1002, NULL,
       DATEADD(DAY, x, DATE '2030-12-31'), DATEADD(DAY, x + 1, DATE '2030-12-31'), 2, 150.00, 'CANCELLED',
       'ONLINE', TIMESTAMP '2030-05-01 10:00:00', TIMESTAMP '2030-05-02 10:00:00'
FROM SYSTEM_RANGE(1, 101);
