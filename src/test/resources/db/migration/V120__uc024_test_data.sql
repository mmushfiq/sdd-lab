-- Test data for UC-024 Erase Guest Data. Tests run with today = 2030-06-01 (Europe/Zurich).
-- Erika has only closed reservations in April 2029, outside the periods of the UC-019 and UC-020 tests. Otto has a
-- Confirmed reservation for the inactive room type Mansion (V118) in September 2031. Tests restore both guests.

INSERT INTO guest (id, first_name, last_name, email, phone)
VALUES (3201, 'Erika', 'Erase', 'erika.erase@example.com', '+41 79 320 32 01'),
       (3202, 'Otto', 'Open', 'otto.open@example.com', '+41 79 320 32 02');

INSERT INTO reservation (id, reservation_code, guest_id, room_type_id, room_id, arrival_date, departure_date,
                         number_of_guests, total_price, status, channel, created_at, checked_in_at, checked_out_at,
                         cancelled_at)
VALUES (3201, 'ERASE00001', 3201, 3001, 3001, '2029-04-10', '2029-04-12', 2, 400.00, 'CHECKED_OUT', 'ONLINE',
        '2029-03-01 10:00:00', '2029-04-10 15:00:00', '2029-04-12 10:00:00', NULL),
       (3202, 'ERASE00002', 3201, 3001, NULL, '2029-04-20', '2029-04-21', 1, 200.00, 'CANCELLED', 'ONLINE',
        '2029-03-01 10:00:00', NULL, NULL, '2029-03-02 10:00:00'),
       (3203, 'ERASE00003', 3201, 3001, NULL, '2029-04-25', '2029-04-26', 1, 200.00, 'NO_SHOW', 'RECEPTION',
        '2029-03-01 10:00:00', NULL, NULL, NULL),
       (3204, 'ERASE00004', 3202, 3001, NULL, '2031-09-01', '2031-09-03', 2, 400.00, 'CONFIRMED', 'ONLINE',
        '2030-05-01 10:00:00', NULL, NULL, NULL),
       (3205, 'ERASE00005', 3202, 3001, 3001, '2029-04-01', '2029-04-02', 2, 200.00, 'CHECKED_OUT', 'ONLINE',
        '2029-03-01 10:00:00', '2029-04-01 15:00:00', '2029-04-02 10:00:00', NULL);
