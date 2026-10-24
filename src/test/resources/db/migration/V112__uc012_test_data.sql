-- Test data for UC-012 Record No-Show. Tests run with today = 2030-06-01 (Europe/Zurich).
-- No other test data has a Confirmed reservation arriving before 2030-06-01, so these are the only missed arrivals.
-- The room type Igloo is inactive (never offered) and has a single room, so its availability shows the released
-- nights.

INSERT INTO room_type (id, name, description, max_guests, nightly_rate, active)
VALUES (2401, 'Igloo', 'An igloo that is no longer offered', 2, 80.00, FALSE);

INSERT INTO room (id, room_number, room_type_id, cleaning_status, active)
VALUES (2401, 'B01', 2401, 'CLEAN', TRUE);

INSERT INTO guest (id, first_name, last_name, email, phone)
VALUES (2401, 'Nora', 'Nichtda', 'nora.nichtda@example.com', '+41 79 240 24 01'),
       (2402, 'Emil', 'Ehemals', 'emil.ehemals@example.com', '+41 79 240 24 02');

INSERT INTO reservation (id, reservation_code, guest_id, room_type_id, room_id, arrival_date, departure_date,
                         number_of_guests, total_price, status, channel, created_at, cancelled_at)
VALUES
    -- Missed arrival on 30 May; the only Igloo is reserved up to the night of 2 June
    (2401, 'MISSED0001', 2401, 2401, NULL, '2030-05-30', '2030-06-03', 2, 320.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL),
    -- Missed arrival on 31 May (Loft, V108), departure today
    (2402, 'MISSED0002', 2402, 2001, NULL, '2030-05-31', '2030-06-01', 1, 160.00, 'CONFIRMED', 'RECEPTION',
     '2030-05-01 10:00:00', NULL),
    -- Not missed: arriving today
    (2403, 'MISSED0003', 2402, 2001, NULL, '2030-06-01', '2030-06-02', 1, 160.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL),
    -- Not missed: cancelled
    (2404, 'MISSED0004', 2402, 2001, NULL, '2030-05-30', '2030-05-31', 1, 160.00, 'CANCELLED', 'ONLINE',
     '2030-05-01 10:00:00', '2030-05-02 10:00:00');
