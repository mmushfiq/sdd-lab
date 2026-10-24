-- Test data for UC-002 View Own Reservation and UC-003 Cancel Own Reservation. Tests run with today = 2030-06-01,
-- 12:00 hotel time (Europe/Zurich). The dates do not touch the dates the UC-001 tests search.

INSERT INTO guest (id, first_name, last_name, email, phone)
VALUES (1101, 'Carla', 'Lookup', 'carla.lookup@example.com', '+41 79 444 44 44'),
       -- A guest whose data was erased (UC-024); her former email address was dora.erased@example.com
       (1102, 'Erased', 'Erased', 'guest-1102@erased.invalid', '000');

INSERT INTO reservation (id, reservation_code, guest_id, room_type_id, room_id, arrival_date, departure_date,
                         number_of_guests, total_price, status, channel, created_at, checked_in_at, checked_out_at,
                         cancelled_at)
VALUES
    -- Confirmed, long before the online cancellation deadline (Double Room, 3 nights)
    (1101, 'LOOKUP0001', 1101, 1002, NULL, '2030-11-01', '2030-11-04', 2, 450.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL, NULL, NULL),
    -- Confirmed, deadline 2030-06-01 14:00 not yet passed at 12:00 (Single Room)
    (1102, 'LOOKUP0002', 1101, 1001, NULL, '2030-06-02', '2030-06-03', 1, 100.00, 'CONFIRMED', 'RECEPTION',
     '2030-05-01 10:00:00', NULL, NULL, NULL),
    -- Confirmed, arriving today: deadline 2030-05-31 14:00 has passed (Single Room)
    (1103, 'LOOKUP0003', 1101, 1001, NULL, '2030-06-01', '2030-06-02', 1, 100.00, 'CONFIRMED', 'RECEPTION',
     '2030-05-01 10:00:00', NULL, NULL, NULL),
    -- Cancelled
    (1104, 'LOOKUP0004', 1101, 1002, NULL, '2030-12-01', '2030-12-03', 2, 300.00, 'CANCELLED', 'ONLINE',
     '2030-05-01 10:00:00', NULL, NULL, '2030-05-02 10:00:00'),
    -- Checked Out
    (1105, 'LOOKUP0005', 1101, 1003, 1005, '2030-05-10', '2030-05-12', 4, 500.00, 'CHECKED_OUT', 'ONLINE',
     '2030-04-01 10:00:00', '2030-05-10 15:00:00', '2030-05-12 10:00:00', NULL),
    -- No-Show
    (1106, 'LOOKUP0006', 1101, 1001, NULL, '2030-05-20', '2030-05-21', 1, 100.00, 'NO_SHOW', 'ONLINE',
     '2030-04-01 10:00:00', NULL, NULL, NULL),
    -- Reservation of the erased guest
    (1107, 'ERASED0001', 1102, 1002, NULL, '2030-11-10', '2030-11-12', 2, 300.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL, NULL, NULL);
