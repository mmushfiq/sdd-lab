-- Test data for UC-017 Maintain Rooms. Tests run with today = 2030-06-01 (Europe/Zurich).
-- The room type Bungalow is inactive, so searches never offer it. Its checked-in guest stays until the end of 2031,
-- so the reservation is neither a departure nor overdue on the dates the UC-008 tests use.

INSERT INTO room_type (id, name, description, max_guests, nightly_rate, active)
VALUES (2701, 'Bungalow', 'A bungalow that is no longer offered', 2, 130.00, FALSE);

INSERT INTO room (id, room_number, room_type_id, cleaning_status, active)
VALUES (2701, 'E01', 2701, 'CLEAN', TRUE),
       (2702, 'E02', 2701, 'CLEAN', TRUE),
       (2703, 'E03', 2701, 'CLEAN', TRUE);

-- E02 is blocked for the night of 20 June 2030
INSERT INTO room_block (id, room_id, start_date, end_date, reason)
VALUES (2701, 2702, '2030-06-20', '2030-06-20', 'Roof repair');

INSERT INTO guest (id, first_name, last_name, email, phone)
VALUES (2701, 'Bea', 'Bungalow', 'bea.bungalow@example.com', '+41 79 270 27 01');

INSERT INTO reservation (id, reservation_code, guest_id, room_type_id, room_id, arrival_date, departure_date,
                         number_of_guests, total_price, status, channel, created_at, checked_in_at)
VALUES
    -- Two Confirmed reservations for the night of 10 June, one of them also for 11 June
    (2701, 'BUNGAL0001', 2701, 2701, NULL, '2030-06-10', '2030-06-12', 2, 260.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL),
    (2702, 'BUNGAL0002', 2701, 2701, NULL, '2030-06-10', '2030-06-11', 2, 130.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL),
    -- Checked In to E03 until the end of 2031
    (2703, 'BUNGAL0003', 2701, 2701, 2703, '2030-05-30', '2031-12-31', 1, 75400.00, 'CHECKED_IN', 'ONLINE',
     '2030-05-01 10:00:00', '2030-05-30 15:00:00'),
    -- Confirmed for the night of 20 June, when E02 is blocked
    (2704, 'BUNGAL0004', 2701, 2701, NULL, '2030-06-20', '2030-06-21', 1, 130.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL);
