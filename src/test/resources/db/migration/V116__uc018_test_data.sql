-- Test data for UC-018 Block Room. Tests run with today = 2030-06-01 (Europe/Zurich).
-- The room type Villa is inactive, so searches never offer it. Its checked-in guest stays until the end of 2031,
-- so the reservation is neither a departure nor overdue on the dates the UC-008 tests use.

INSERT INTO room_type (id, name, description, max_guests, nightly_rate, active)
VALUES (2801, 'Villa', 'A villa that is no longer offered', 4, 400.00, FALSE);

INSERT INTO room (id, room_number, room_type_id, cleaning_status, active)
VALUES (2801, 'F01', 2801, 'CLEAN', TRUE),
       (2802, 'F02', 2801, 'CLEAN', TRUE),
       (2803, 'F03', 2801, 'CLEAN', TRUE),
       (2804, 'F04', 2801, 'CLEAN', FALSE);

-- F01 is blocked from 10 to 12 July 2030; an old block of F02 lies in the past
INSERT INTO room_block (id, room_id, start_date, end_date, reason)
VALUES (2801, 2801, '2030-07-10', '2030-07-12', 'Painting'),
       (2802, 2802, '2030-05-01', '2030-05-03', 'Old repair');

INSERT INTO guest (id, first_name, last_name, email, phone)
VALUES (2801, 'Vera', 'Villa', 'vera.villa@example.com', '+41 79 280 28 01');

INSERT INTO reservation (id, reservation_code, guest_id, room_type_id, room_id, arrival_date, departure_date,
                         number_of_guests, total_price, status, channel, created_at, checked_in_at)
VALUES
    -- Checked In to F03 until the end of 2031
    (2801, 'VILLA00001', 2801, 2801, 2803, '2030-05-30', '2031-12-31', 1, 230000.00, 'CHECKED_IN', 'ONLINE',
     '2030-05-01 10:00:00', '2030-05-30 15:00:00'),
    -- Two Confirmed reservations for the nights of 15 and 16 June: with VILLA00001 the Villa is full
    (2802, 'VILLA00002', 2801, 2801, NULL, '2030-06-15', '2030-06-17', 2, 800.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL),
    (2803, 'VILLA00003', 2801, 2801, NULL, '2030-06-15', '2030-06-17', 2, 800.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL),
    -- One Confirmed reservation for the night of 25 June: with VILLA00001 one room is left
    (2804, 'VILLA00004', 2801, 2801, NULL, '2030-06-25', '2030-06-26', 2, 400.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL);

-- Cancelled reservations do not take a room
INSERT INTO reservation (id, reservation_code, guest_id, room_type_id, room_id, arrival_date, departure_date,
                         number_of_guests, total_price, status, channel, created_at, checked_in_at, cancelled_at)
VALUES (2805, 'VILLA00005', 2801, 2801, NULL, '2030-06-25', '2030-06-26', 2, 400.00, 'CANCELLED', 'ONLINE',
        '2030-05-01 10:00:00', NULL, '2030-05-02 10:00:00');
