-- Test data for UC-013 View Cleaning List and UC-014 Mark Room Clean. Tests run with today = 2030-06-01
-- (Europe/Zurich). The room type Pod is inactive, so searches never offer it. The checked-in guests either depart
-- today or stay until the end of 2031.

INSERT INTO room_type (id, name, description, max_guests, nightly_rate, active)
VALUES (2501, 'Pod', 'A pod that is no longer offered', 1, 60.00, FALSE);

INSERT INTO room (id, room_number, room_type_id, cleaning_status, active)
VALUES (2501, 'C01', 2501, 'DIRTY', TRUE),
       (2502, 'C02', 2501, 'DIRTY', TRUE),
       (2503, 'C03', 2501, 'DIRTY', TRUE),
       (2504, 'C04', 2501, 'DIRTY', TRUE),
       (2505, 'C05', 2501, 'DIRTY', TRUE),
       (2506, 'C06', 2501, 'CLEAN', TRUE),
       (2507, 'C07', 2501, 'DIRTY', FALSE),
       (2508, 'C08', 2501, 'DIRTY', TRUE);

-- C05 is blocked today
INSERT INTO room_block (id, room_id, start_date, end_date, reason)
VALUES (2501, 2505, '2030-06-01', '2030-06-02', 'Pest control');

INSERT INTO guest (id, first_name, last_name, email, phone)
VALUES (2501, 'Hans', 'Haushalt', 'hans.haushalt@example.com', '+41 79 250 25 01');

INSERT INTO reservation (id, reservation_code, guest_id, room_type_id, room_id, arrival_date, departure_date,
                         number_of_guests, total_price, status, channel, created_at, checked_in_at, checked_out_at)
VALUES
    -- C01: the guest checked out today
    (2501, 'CLEAN00001', 2501, 2501, 2501, '2030-05-29', '2030-06-01', 1, 180.00, 'CHECKED_OUT', 'ONLINE',
     '2030-05-01 10:00:00', '2030-05-29 15:00:00', '2030-06-01 09:00:00'),
    -- C02: occupied, the guest departs today
    (2502, 'CLEAN00002', 2501, 2501, 2502, '2030-05-30', '2030-06-01', 1, 120.00, 'CHECKED_IN', 'ONLINE',
     '2030-05-01 10:00:00', '2030-05-30 15:00:00', NULL),
    -- C04: occupied until the end of 2031
    (2503, 'CLEAN00003', 2501, 2501, 2504, '2030-05-30', '2031-12-31', 1, 34800.00, 'CHECKED_IN', 'ONLINE',
     '2030-05-01 10:00:00', '2030-05-30 15:00:00', NULL),
    -- C08: the guest checked out yesterday
    (2504, 'CLEAN00004', 2501, 2501, 2508, '2030-05-28', '2030-05-31', 1, 180.00, 'CHECKED_OUT', 'ONLINE',
     '2030-05-01 10:00:00', '2030-05-28 15:00:00', '2030-05-31 10:00:00');
