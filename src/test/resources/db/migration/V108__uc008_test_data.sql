-- Test data for UC-008 View Arrivals and Departures. The tests run with today = 2031-07-20 (Europe/Zurich).
-- All reservations are for the inactive Loft, which searches never offer, so the availability other tests see is
-- unchanged; none of them is overdue on the 2030-06-01 that the other tests use.

INSERT INTO room_type (id, name, description, max_guests, nightly_rate, active)
VALUES (2001, 'Loft', 'A loft that is no longer offered', 2, 160.00, FALSE);

INSERT INTO room (id, room_number, room_type_id, cleaning_status, active)
VALUES (2001, '601', 2001, 'CLEAN', TRUE),
       (2002, '602', 2001, 'DIRTY', TRUE),
       (2003, '603', 2001, 'CLEAN', TRUE),
       (2004, '604', 2001, 'CLEAN', TRUE);

INSERT INTO guest (id, first_name, last_name, email, phone)
VALUES (2001, 'Zora', 'Zeller', 'zora.zeller@example.com', '+41 79 200 20 01'),
       (2002, 'Adam', 'Ankunft', 'adam.ankunft@example.com', '+41 79 200 20 02'),
       (2003, 'Doris', 'Abreise', 'doris.abreise@example.com', '+41 79 200 20 03'),
       (2004, 'Bruno', 'Weg', 'bruno.weg@example.com', '+41 79 200 20 04'),
       (2005, 'Otto', 'Spaet', 'otto.spaet@example.com', '+41 79 200 20 05');

INSERT INTO reservation (id, reservation_code, guest_id, room_type_id, room_id, arrival_date, departure_date,
                         number_of_guests, total_price, status, channel, created_at, checked_in_at, checked_out_at,
                         cancelled_at)
VALUES
    -- Arrivals on 20 July 2031
    (2001, 'ARRIVE0001', 2001, 2001, NULL, '2031-07-20', '2031-07-23', 2, 480.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL, NULL, NULL),
    (2002, 'ARRIVE0002', 2002, 2001, 2001, '2031-07-20', '2031-07-21', 1, 160.00, 'CHECKED_IN', 'RECEPTION',
     '2030-05-01 10:00:00', '2031-07-20 13:00:00', NULL, NULL),
    -- Not an arrival: cancelled
    (2003, 'ARRIVE0003', 2001, 2001, NULL, '2031-07-20', '2031-07-22', 2, 320.00, 'CANCELLED', 'ONLINE',
     '2030-05-01 10:00:00', NULL, NULL, '2030-05-02 10:00:00'),
    -- Departures on 20 July 2031
    (2004, 'DEPART0001', 2003, 2001, 2003, '2031-07-17', '2031-07-20', 2, 480.00, 'CHECKED_IN', 'ONLINE',
     '2030-05-01 10:00:00', '2031-07-17 15:00:00', NULL, NULL),
    (2005, 'DEPART0002', 2004, 2001, 2002, '2031-07-18', '2031-07-20', 1, 320.00, 'CHECKED_OUT', 'ONLINE',
     '2030-05-01 10:00:00', '2031-07-18 15:00:00', '2031-07-20 09:00:00', NULL),
    -- Overdue: still checked in, departure date 19 July 2031
    (2006, 'DEPART0003', 2005, 2001, 2004, '2031-07-15', '2031-07-19', 2, 640.00, 'CHECKED_IN', 'ONLINE',
     '2030-05-01 10:00:00', '2031-07-15 15:00:00', NULL, NULL),
    -- Not a departure: no-show
    (2007, 'DEPART0004', 2005, 2001, NULL, '2031-07-19', '2031-07-20', 1, 160.00, 'NO_SHOW', 'ONLINE',
     '2030-05-01 10:00:00', NULL, NULL, NULL);
