-- Test data for UC-005 Change Reservation. Tests run with today = 2030-06-01 (Europe/Zurich).
-- Confirmed reservations are in May 2031, which no other test searches. The checked-in guest stays in the
-- inactive Studio, a room type that searches never offer, so the availability other tests see is unchanged.

INSERT INTO room_type (id, name, description, max_guests, nightly_rate, active)
VALUES (1801, 'Studio', 'A studio that is no longer offered', 2, 120.00, FALSE);

INSERT INTO room (id, room_number, room_type_id, cleaning_status, active)
VALUES (1801, '501', 1801, 'CLEAN', TRUE),
       (1802, '502', 1801, 'CLEAN', TRUE);

-- Room 501 is blocked for the night of 5 June 2030
INSERT INTO room_block (id, room_id, start_date, end_date, reason)
VALUES (1801, 1801, '2030-06-05', '2030-06-05', 'Painting');

INSERT INTO guest (id, first_name, last_name, email, phone)
VALUES (1801, 'Clemens', 'Change', 'clemens.change@example.com', '+41 79 180 18 01'),
       (1802, 'Olga', 'Other', 'olga.other@example.com', '+41 79 180 18 02');

INSERT INTO reservation (id, reservation_code, guest_id, room_type_id, room_id, arrival_date, departure_date,
                         number_of_guests, total_price, status, channel, created_at, checked_in_at, cancelled_at)
VALUES
    -- Confirmed Double Room, 2 nights at 150.00
    (1801, 'CHANGE0001', 1801, 1002, NULL, '2031-05-10', '2031-05-12', 2, 300.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL, NULL),
    -- Confirmed Double Room, reserved at an older nightly rate of 120.00
    (1802, 'CHANGE0002', 1801, 1002, NULL, '2031-05-15', '2031-05-16', 2, 120.00, 'CONFIRMED', 'RECEPTION',
     '2030-05-01 10:00:00', NULL, NULL),
    -- Confirmed, the only Family Room for the nights of 20 and 21 May 2031
    (1803, 'CHANGE0003', 1801, 1003, NULL, '2031-05-20', '2031-05-22', 3, 500.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL, NULL),
    -- Another guest has the only Family Room for the night of 25 May 2031
    (1804, 'CHANGE0004', 1802, 1003, NULL, '2031-05-25', '2031-05-26', 2, 250.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL, NULL),
    -- Cancelled
    (1805, 'CHANGE0005', 1801, 1002, NULL, '2031-05-01', '2031-05-02', 2, 150.00, 'CANCELLED', 'ONLINE',
     '2030-05-01 10:00:00', NULL, '2030-05-02 10:00:00'),
    -- Confirmed reservation of the inactive Studio
    (1806, 'CHANGE0006', 1801, 1801, NULL, '2031-05-05', '2031-05-06', 2, 120.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL, NULL),
    -- Checked In to room 501 (Studio) for 4 nights at 120.00
    (1810, 'CHANGE0010', 1801, 1801, 1801, '2030-05-30', '2030-06-03', 2, 480.00, 'CHECKED_IN', 'ONLINE',
     '2030-05-01 10:00:00', '2030-05-30 15:00:00', NULL),
    -- Both Studios are reserved by other guests for the night of 7 June 2030
    (1811, 'CHANGE0011', 1802, 1801, NULL, '2030-06-07', '2030-06-08', 1, 120.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL, NULL),
    (1812, 'CHANGE0012', 1802, 1801, NULL, '2030-06-07', '2030-06-08', 1, 120.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL, NULL);

-- CR-001: CHANGE0002 was recorded before the 48-hour deadline took effect and keeps its 24-hour deadline until its
-- arrival date is changed (UC-005 BR-007). The other reservations get 48 hours (V199).
UPDATE reservation SET online_cancellation_deadline = '2031-05-14 14:00:00' WHERE id = 1802;
