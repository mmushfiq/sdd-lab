-- Test data for UC-011 View Room Status Board. Tests run with today = 2030-06-01 (Europe/Zurich).
-- The room type Tent is inactive, so searches never offer it. The checked-in guest stays until the end of 2031, so
-- the reservation is neither a departure nor overdue on the dates the UC-008 tests use.

INSERT INTO room_type (id, name, description, max_guests, nightly_rate, active)
VALUES (2301, 'Tent', 'A tent that is no longer offered', 2, 50.00, FALSE);

INSERT INTO room (id, room_number, room_type_id, cleaning_status, active)
VALUES (2301, 'A01', 2301, 'CLEAN', TRUE),
       (2302, 'A02', 2301, 'DIRTY', TRUE),
       (2303, 'A03', 2301, 'CLEAN', FALSE);

INSERT INTO room_block (id, room_id, start_date, end_date, reason)
VALUES
    -- A01 is blocked from 31 May to 3 June 2030
    (2301, 2301, '2030-05-31', '2030-06-03', 'Renovation'),
    -- A02 is occupied and also blocked today
    (2302, 2302, '2030-06-01', '2030-06-01', 'Inspection');

INSERT INTO guest (id, first_name, last_name, email, phone)
VALUES (2301, 'Lena', 'Lager', 'lena.lager@example.com', '+41 79 230 23 01');

INSERT INTO reservation (id, reservation_code, guest_id, room_type_id, room_id, arrival_date, departure_date,
                         number_of_guests, total_price, status, channel, created_at, checked_in_at)
VALUES (2301, 'OCCUPY0003', 2301, 2301, 2302, '2030-05-30', '2031-12-31', 1, 29050.00, 'CHECKED_IN', 'ONLINE',
        '2030-05-01 10:00:00', '2030-05-30 15:00:00');
