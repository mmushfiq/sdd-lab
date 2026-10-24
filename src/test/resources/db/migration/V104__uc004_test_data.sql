-- Test data for UC-004 Create Reservation. Tests run with today = 2030-06-01 (Europe/Zurich).
-- The tests search January to March 2031, dates that the UC-001, UC-002 and UC-003 tests do not touch.

INSERT INTO guest (id, first_name, last_name, email, phone)
VALUES (1401, 'Felix', 'Bekannt', 'felix.bekannt@example.com', '+41 79 666 66 66');

INSERT INTO reservation (id, reservation_code, guest_id, room_type_id, room_id, arrival_date, departure_date,
                         number_of_guests, total_price, status, channel, created_at, cancelled_at)
VALUES
    -- One of the two active Double Rooms is reserved for the nights of 10 and 11 February 2031
    (1401, 'RECEPT0001', 1401, 1002, NULL, '2031-02-10', '2031-02-12', 2, 300.00, 'CONFIRMED', 'RECEPTION',
     '2030-05-01 10:00:00', NULL);
