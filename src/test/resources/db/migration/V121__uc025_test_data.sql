-- Test data for UC-025 Maintain Seasonal Rates (CR-002) and the season pricing of UC-001, UC-004, UC-005 and UC-010.
-- Tests run with today = 2030-06-01 (Europe/Zurich).
-- The seasons set season rates only for the inactive room type Riad, so no other test sees a season price. Tests
-- that search the Riad switch it to active and back. Its rooms are blocked on 2029-01-10 like all other active rooms
-- (UC-019 A2, V117).

INSERT INTO room_type (id, name, description, max_guests, nightly_rate, active)
VALUES (3301, 'Riad', 'A riad around a quiet courtyard', 2, 100.00, FALSE);

INSERT INTO room (id, room_number, room_type_id, cleaning_status, active)
VALUES (3301, 'R01', 3301, 'CLEAN', TRUE),
       (3302, 'R02', 3301, 'CLEAN', TRUE),
       (3303, 'R03', 3301, 'CLEAN', TRUE);

INSERT INTO room_block (id, room_id, start_date, end_date, reason)
VALUES (3301, 3301, '2029-01-10', '2029-01-10', 'Occupancy report test'),
       (3302, 3302, '2029-01-10', '2029-01-10', 'Occupancy report test'),
       (3303, 3303, '2029-01-10', '2029-01-10', 'Occupancy report test');

-- Late May 2030: the Riad costs 180.00; Autumn 2030: 160.00 from 5 to 7 November. Neither sets a rate for any
-- other room type.
INSERT INTO season (id, name, first_night, last_night)
VALUES (3301, 'Late May 2030', '2030-05-30', '2030-05-31'),
       (3302, 'Autumn 2030', '2030-11-05', '2030-11-07');

INSERT INTO season_rate (season_id, room_type_id, nightly_rate)
VALUES (3301, 3301, 180.00),
       (3302, 3301, 160.00);

INSERT INTO guest (id, first_name, last_name, email, phone)
VALUES (3301, 'Selma', 'Saison', 'selma.saison@example.com', '+41 79 330 33 01');

INSERT INTO reservation (id, reservation_code, guest_id, room_type_id, room_id, arrival_date, departure_date,
                         number_of_guests, total_price, status, channel, created_at, checked_in_at, checked_out_at)
VALUES
    -- Overdue: reserved until 30 May, 3 nights at the normal rate in room R01
    (3301, 'RIAD000001', 3301, 3301, 3301, '2030-05-27', '2030-05-30', 1, 300.00, 'CHECKED_IN', 'ONLINE',
     '2030-05-01 10:00:00', '2030-05-27 15:00:00', NULL),
    -- Reserved until 4 June in room R02, with the season rate on 30 and 31 May
    (3302, 'RIAD000002', 3301, 3301, 3302, '2030-05-29', '2030-06-04', 2, 760.00, 'CHECKED_IN', 'RECEPTION',
     '2030-05-01 10:00:00', '2030-05-29 15:00:00', NULL),
    -- Confirmed for 3 and 4 November, recorded at an older rate of 90.00
    (3303, 'RIAD000003', 3301, 3301, NULL, '2030-11-03', '2030-11-05', 2, 180.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL, NULL),
    -- Confirmed for 5 and 6 November, recorded before the Autumn 2030 season existed
    (3304, 'RIAD000004', 3301, 3301, NULL, '2030-11-05', '2030-11-07', 2, 200.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL, NULL);

-- RIAD000001 gets its nights from V198 (3 x 100.00).
INSERT INTO reservation_night (reservation_id, night_date, night_price)
VALUES (3302, '2030-05-29', 100.00),
       (3302, '2030-05-30', 180.00),
       (3302, '2030-05-31', 180.00),
       (3302, '2030-06-01', 100.00),
       (3302, '2030-06-02', 100.00),
       (3302, '2030-06-03', 100.00),
       (3303, '2030-11-03', 90.00),
       (3303, '2030-11-04', 90.00),
       (3304, '2030-11-05', 100.00),
       (3304, '2030-11-06', 100.00);
