-- Test data for UC-019 View Occupancy Report. Tests run with today = 2030-06-01 (Europe/Zurich).
-- The report period of the tests is January and February 2029; no other test data has reservations or room blocks
-- before 2030. The room type Dormitory is inactive, so searches never offer it, and its rooms are clean.

INSERT INTO room_type (id, name, description, max_guests, nightly_rate, active)
VALUES (2901, 'Dormitory', 'A dormitory that is no longer offered', 2, 50.00, FALSE);

INSERT INTO room (id, room_number, room_type_id, cleaning_status, active)
VALUES (2901, 'G01', 2901, 'CLEAN', TRUE),
       (2902, 'G02', 2901, 'CLEAN', TRUE),
       (2903, 'G03', 2901, 'CLEAN', TRUE),
       (2904, 'G04', 2901, 'CLEAN', FALSE);

-- On 10 January 2029 every active room is blocked, so there are no available room nights (A2).
-- Test data of later use cases that adds active rooms must block them on this day as well.
INSERT INTO room_block (id, room_id, start_date, end_date, reason)
SELECT 290000 + id, id, '2029-01-10', '2029-01-10', 'Hotel closed'
FROM room
WHERE active = TRUE;

-- G03 is blocked for the nights of 31 January and 1 February 2029; the block of the inactive room G04 on
-- 31 January does not change the available room nights.
INSERT INTO room_block (id, room_id, start_date, end_date, reason)
VALUES (2901, 2903, '2029-01-31', '2029-02-01', 'Repairs'),
       (2902, 2904, '2029-01-31', '2029-01-31', 'Repairs');

INSERT INTO guest (id, first_name, last_name, email, phone)
VALUES (2901, 'Otto', 'Occupancy', 'otto.occupancy@example.com', '+41 79 290 29 01');

INSERT INTO reservation (id, reservation_code, guest_id, room_type_id, room_id, arrival_date, departure_date,
                         number_of_guests, total_price, status, channel, created_at, checked_in_at, checked_out_at,
                         cancelled_at)
VALUES
    -- Checked Out from G01: the nights of 30 and 31 January and 1 February 2029
    (2901, 'OCCUP00001', 2901, 2901, 2901, '2029-01-30', '2029-02-02', 1, 150.00, 'CHECKED_OUT', 'ONLINE',
     '2029-01-01 10:00:00', '2029-01-30 15:00:00', '2029-02-02 10:00:00', NULL),
    -- Checked Out from G02: the night of 31 January 2029
    (2902, 'OCCUP00002', 2901, 2901, 2902, '2029-01-31', '2029-02-01', 1, 50.00, 'CHECKED_OUT', 'RECEPTION',
     '2029-01-01 10:00:00', '2029-01-31 15:00:00', '2029-02-01 10:00:00', NULL),
    -- Cancelled and No-Show reservations are no occupied room nights
    (2903, 'OCCUP00003', 2901, 2901, NULL, '2029-01-30', '2029-02-01', 1, 100.00, 'CANCELLED', 'ONLINE',
     '2029-01-01 10:00:00', NULL, NULL, '2029-01-02 10:00:00'),
    (2904, 'OCCUP00004', 2901, 2901, NULL, '2029-01-31', '2029-02-01', 1, 50.00, 'NO_SHOW', 'ONLINE',
     '2029-01-01 10:00:00', NULL, NULL, NULL);
