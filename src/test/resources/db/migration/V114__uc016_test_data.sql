-- Test data for UC-016 Maintain Room Types. The Garden Room is inactive, so the searches of other tests never offer
-- it; the tests activate it only for the time of a test.

INSERT INTO room_type (id, name, description, max_guests, nightly_rate, active)
VALUES (2601, 'Garden Room', 'A room with a view of the garden', 3, 200.00, FALSE);

INSERT INTO room (id, room_number, room_type_id, cleaning_status, active)
VALUES (2601, 'D01', 2601, 'CLEAN', TRUE),
       (2602, 'D02', 2601, 'CLEAN', TRUE),
       (2603, 'D03', 2601, 'CLEAN', FALSE);

INSERT INTO guest (id, first_name, last_name, email, phone)
VALUES (2601, 'Greta', 'Garten', 'greta.garten@example.com', '+41 79 260 26 01');

INSERT INTO reservation (id, reservation_code, guest_id, room_type_id, room_id, arrival_date, departure_date,
                         number_of_guests, total_price, status, channel, created_at, cancelled_at)
VALUES
    -- Confirmed with 3 guests
    (2601, 'GARDEN0001', 2601, 2601, NULL, '2031-08-10', '2031-08-12', 3, 400.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL),
    -- Cancelled with 3 guests: does not count
    (2602, 'GARDEN0002', 2601, 2601, NULL, '2031-08-20', '2031-08-21', 3, 200.00, 'CANCELLED', 'ONLINE',
     '2030-05-01 10:00:00', '2030-05-02 10:00:00'),
    -- Confirmed with 2 guests
    (2603, 'GARDEN0003', 2601, 2601, NULL, '2031-08-15', '2031-08-16', 2, 200.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL);
