-- Test data for UC-001 Reserve Room Online. Tests run with today = 2030-06-01.
-- IDs start at 1001 so rows created by tests (sequence values below 1000) can be told apart.

INSERT INTO room_type (id, name, description, max_guests, nightly_rate, active)
VALUES (1001, 'Single Room', 'A quiet room with one bed', 1, 100.00, TRUE),
       (1002, 'Double Room', 'A room with a queen-size bed', 2, 150.00, TRUE),
       (1003, 'Family Room', 'A large room with two double beds', 4, 250.00, TRUE),
       (1004, 'Suite', 'A suite that is not offered at the moment', 4, 400.00, FALSE);

-- Single Room: 1 room; Double Room: 2 active rooms and 1 inactive room; Family Room: 1 room; Suite: 1 room
INSERT INTO room (id, room_number, room_type_id, cleaning_status, active)
VALUES (1001, '101', 1001, 'CLEAN', TRUE),
       (1002, '201', 1002, 'CLEAN', TRUE),
       (1003, '202', 1002, 'CLEAN', TRUE),
       (1004, '203', 1002, 'CLEAN', FALSE),
       (1005, '301', 1003, 'CLEAN', TRUE),
       (1006, '401', 1004, 'CLEAN', TRUE);

-- The only Family Room is blocked for the nights of 10, 11 and 12 July 2030
INSERT INTO room_block (id, room_id, start_date, end_date, reason)
VALUES (1001, 1005, '2030-07-10', '2030-07-12', 'Repairs');

INSERT INTO guest (id, first_name, last_name, email, phone)
VALUES (1001, 'Anna', 'Muster', 'anna.muster@example.com', '+41 79 111 11 11'),
       (1002, 'Ben', 'Beispiel', 'ben.beispiel@example.com', '+41 79 222 22 22');

INSERT INTO reservation (id, reservation_code, guest_id, room_type_id, room_id, arrival_date, departure_date,
                         number_of_guests, total_price, status, channel, created_at, cancelled_at)
VALUES
    -- The only Single Room is reserved for the nights of 1 and 2 August 2030
    (1001, 'TESTSGL001', 1001, 1001, NULL, '2030-08-01', '2030-08-03', 1, 200.00, 'CONFIRMED', 'RECEPTION',
     '2030-05-01 10:00:00', NULL),
    -- 1-3 September 2030: one Confirmed and one Cancelled Double Room reservation, so one Double Room is still free
    (1002, 'TESTDBL001', 1001, 1002, NULL, '2030-09-01', '2030-09-03', 2, 300.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL),
    (1003, 'TESTDBL002', 1001, 1002, NULL, '2030-09-01', '2030-09-03', 2, 300.00, 'CANCELLED', 'ONLINE',
     '2030-05-01 10:00:00', '2030-05-02 10:00:00'),
    -- 1-3 October 2030: both active Double Rooms are reserved (the inactive one does not count)
    (1004, 'TESTDBL003', 1001, 1002, NULL, '2030-10-01', '2030-10-03', 2, 300.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL),
    (1005, 'TESTDBL004', 1001, 1002, NULL, '2030-10-01', '2030-10-03', 2, 300.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL);
