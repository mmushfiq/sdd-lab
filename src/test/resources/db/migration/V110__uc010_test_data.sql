-- Test data for UC-010 Check Out Guest. Tests run with today = 2030-06-01 12:00 (Europe/Zurich).
-- The room type Chalet is inactive, so searches never offer it. No reservation arrives or departs on 2030-05-25,
-- the date the UC-008 tests use for empty lists.

INSERT INTO room_type (id, name, description, max_guests, nightly_rate, active)
VALUES (2201, 'Chalet', 'A chalet that is no longer offered', 2, 150.00, FALSE);

INSERT INTO room (id, room_number, room_type_id, cleaning_status, active)
VALUES (2201, '901', 2201, 'CLEAN', TRUE),
       (2202, '902', 2201, 'DIRTY', TRUE),
       (2203, '903', 2201, 'CLEAN', TRUE),
       (2204, '904', 2201, 'CLEAN', TRUE);

INSERT INTO guest (id, first_name, last_name, email, phone)
VALUES (2201, 'Paula', 'Auschecken', 'paula.auschecken@example.com', '+41 79 220 22 01');

INSERT INTO reservation (id, reservation_code, guest_id, room_type_id, room_id, arrival_date, departure_date,
                         number_of_guests, total_price, status, channel, created_at, checked_in_at, checked_out_at)
VALUES
    -- Departs today: 3 nights in room 901
    (2201, 'CHECKOUT01', 2201, 2201, 2201, '2030-05-29', '2030-06-01', 2, 450.00, 'CHECKED_IN', 'ONLINE',
     '2030-05-01 10:00:00', '2030-05-29 15:00:00', NULL),
    -- Reserved until 4 June: 5 nights for 500.00 in room 902, which is already dirty
    (2202, 'CHECKOUT02', 2201, 2201, 2202, '2030-05-30', '2030-06-04', 2, 500.00, 'CHECKED_IN', 'RECEPTION',
     '2030-05-01 10:00:00', '2030-05-30 15:00:00', NULL),
    -- Overdue: reserved until 30 May, 3 nights for 300.00 in room 903
    (2203, 'CHECKOUT03', 2201, 2201, 2203, '2030-05-27', '2030-05-30', 1, 300.00, 'CHECKED_IN', 'ONLINE',
     '2030-05-01 10:00:00', '2030-05-27 15:00:00', NULL),
    -- Arrived today for one night in room 904
    (2204, 'CHECKOUT04', 2201, 2201, 2204, '2030-06-01', '2030-06-02', 1, 150.00, 'CHECKED_IN', 'ONLINE',
     '2030-05-01 10:00:00', '2030-06-01 09:00:00', NULL),
    -- Confirmed, not yet checked in
    (2205, 'CHECKOUT05', 2201, 2201, NULL, '2030-06-10', '2030-06-12', 2, 300.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL, NULL);
