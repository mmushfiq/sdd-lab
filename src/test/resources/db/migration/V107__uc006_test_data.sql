-- Test data for UC-006 Cancel Reservation. Tests run with today = 2030-06-01 12:00 (Europe/Zurich).
-- The reservation arriving today is for the inactive Studio (V106), which searches never offer.

INSERT INTO guest (id, first_name, last_name, email, phone)
VALUES (1901, 'Rosa', 'Storno', 'rosa.storno@example.com', '+41 79 190 19 01');

INSERT INTO reservation (id, reservation_code, guest_id, room_type_id, room_id, arrival_date, departure_date,
                         number_of_guests, total_price, status, channel, created_at, cancelled_at)
VALUES
    -- Confirmed Double Room, 2 nights
    (1901, 'CANCELR001', 1901, 1002, NULL, '2031-03-20', '2031-03-22', 2, 300.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL),
    -- Confirmed, arriving today: past the online cancellation deadline
    (1902, 'CANCELR002', 1901, 1801, NULL, '2030-06-01', '2030-06-02', 1, 120.00, 'CONFIRMED', 'RECEPTION',
     '2030-05-01 10:00:00', NULL),
    -- No-Show
    (1903, 'CANCELR003', 1901, 1001, NULL, '2030-05-20', '2030-05-21', 1, 100.00, 'NO_SHOW', 'ONLINE',
     '2030-05-01 10:00:00', NULL);
