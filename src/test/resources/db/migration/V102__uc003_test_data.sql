-- Test data for UC-003 Cancel Own Reservation. Tests start at 2030-06-01 12:00 hotel time (Europe/Zurich).
-- The dates and room types do not touch what the UC-001 and UC-002 tests search.

INSERT INTO guest (id, first_name, last_name, email, phone)
VALUES (1201, 'Emil', 'Cancel', 'emil.cancel@example.com', '+41 79 555 55 55');

INSERT INTO reservation (id, reservation_code, guest_id, room_type_id, room_id, arrival_date, departure_date,
                         number_of_guests, total_price, status, channel, created_at, cancelled_at)
VALUES
    -- Confirmed, long before the deadline; the only Family Room for the nights of 20 and 21 November 2030
    (1201, 'CANCEL0001', 1201, 1003, NULL, '2030-11-20', '2030-11-22', 3, 500.00, 'CONFIRMED', 'ONLINE',
     '2030-05-01 10:00:00', NULL),
    -- Confirmed, arrival tomorrow: the online cancellation deadline is 2030-06-01 14:00 (Double Room)
    (1202, 'CANCEL0002', 1201, 1002, NULL, '2030-06-02', '2030-06-03', 2, 150.00, 'CONFIRMED', 'RECEPTION',
     '2030-05-01 10:00:00', NULL),
    -- Already cancelled
    (1203, 'CANCEL0003', 1201, 1002, NULL, '2030-12-10', '2030-12-12', 2, 300.00, 'CANCELLED', 'ONLINE',
     '2030-05-01 10:00:00', '2030-05-02 10:00:00');
