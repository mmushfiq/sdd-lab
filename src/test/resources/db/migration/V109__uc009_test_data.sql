-- Test data for UC-009 Check In Guest. Tests run with today = 2030-06-01 12:00 (Europe/Zurich).
-- The room types Cabin and Hut are inactive, so searches never offer them. The checked-in guest in room 706 stays
-- until the end of 2031, so the reservation is neither a departure nor overdue on the dates the UC-008 tests use.

INSERT INTO room_type (id, name, description, max_guests, nightly_rate, active)
VALUES (2101, 'Cabin', 'A cabin that is no longer offered', 2, 90.00, FALSE),
       (2102, 'Hut', 'A hut that is no longer offered', 2, 70.00, FALSE);

INSERT INTO room (id, room_number, room_type_id, cleaning_status, active)
VALUES (2101, '701', 2101, 'CLEAN', TRUE),
       (2102, '702', 2101, 'CLEAN', TRUE),
       -- Dirty
       (2103, '703', 2101, 'DIRTY', TRUE),
       -- Blocked for the night of 2 June 2030
       (2104, '704', 2101, 'CLEAN', TRUE),
       -- Inactive
       (2105, '705', 2101, 'CLEAN', FALSE),
       -- Occupied by OCCUPY0001
       (2106, '706', 2101, 'CLEAN', TRUE),
       -- Hut: one dirty room and one occupied clean room
       (2107, '801', 2102, 'DIRTY', TRUE),
       (2108, '802', 2102, 'CLEAN', TRUE);

INSERT INTO room_block (id, room_id, start_date, end_date, reason)
VALUES (2101, 2104, '2030-06-02', '2030-06-02', 'Window repair');

INSERT INTO guest (id, first_name, last_name, email, phone)
VALUES (2101, 'Ines', 'Einchecken', 'ines.einchecken@example.com', '+41 79 210 21 01'),
       (2102, 'Konrad', 'Konkurrent', 'konrad.konkurrent@example.com', '+41 79 210 21 02');

INSERT INTO reservation (id, reservation_code, guest_id, room_type_id, room_id, arrival_date, departure_date,
                         number_of_guests, total_price, status, channel, created_at, checked_in_at, cancelled_at)
VALUES
    -- Confirmed Cabin arriving today for the nights of 1 and 2 June
    (2101, 'CHECKIN001', 2101, 2101, NULL, '2030-06-01', '2030-06-03', 2, 180.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL, NULL),
    -- Another Confirmed Cabin arriving today for one night
    (2102, 'CHECKIN002', 2102, 2101, NULL, '2030-06-01', '2030-06-02', 1, 90.00, 'CONFIRMED', 'RECEPTION',
     '2030-05-01 10:00:00', NULL, NULL),
    -- Confirmed, arriving tomorrow
    (2103, 'CHECKIN003', 2101, 2101, NULL, '2030-06-02', '2030-06-03', 2, 90.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL, NULL),
    -- Cancelled, would have arrived today
    (2104, 'CHECKIN004', 2101, 2101, NULL, '2030-06-01', '2030-06-02', 2, 90.00, 'CANCELLED', 'ONLINE',
     '2030-05-01 10:00:00', NULL, '2030-05-02 10:00:00'),
    -- Confirmed Hut arriving today: no Hut room is ready
    (2105, 'CHECKIN005', 2101, 2102, NULL, '2030-06-01', '2030-06-02', 1, 70.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL, NULL),
    -- Checked In guests in room 706 (Cabin) and room 802 (Hut)
    (2106, 'OCCUPY0001', 2102, 2101, 2106, '2030-05-30', '2031-12-31', 1, 52290.00, 'CHECKED_IN', 'ONLINE',
     '2030-05-01 10:00:00', '2030-05-30 15:00:00', NULL),
    (2107, 'OCCUPY0002', 2102, 2102, 2108, '2030-05-30', '2031-12-31', 1, 40670.00, 'CHECKED_IN', 'ONLINE',
     '2030-05-01 10:00:00', '2030-05-30 15:00:00', NULL);
