-- Test data for UC-020 View Revenue Report. Tests run with today = 2030-06-01 (Europe/Zurich).
-- The report period of the tests is November and December 2028. No other test data has stays before 2029, and no
-- night here lies in 2029, so the UC-019 test period stays untouched. The room type Mansion and its room are
-- inactive, so searches never offer them and they add no available room nights to the UC-019 report.

INSERT INTO room_type (id, name, description, max_guests, nightly_rate, active)
VALUES (3001, 'Mansion', 'A mansion that is no longer offered', 4, 200.00, FALSE);

INSERT INTO room (id, room_number, room_type_id, cleaning_status, active)
VALUES (3001, 'H01', 3001, 'CLEAN', FALSE);

INSERT INTO guest (id, first_name, last_name, email, phone)
VALUES (3001, 'Rita', 'Revenue', 'rita.revenue@example.com', '+41 79 300 30 01');

INSERT INTO reservation (id, reservation_code, guest_id, room_type_id, room_id, arrival_date, departure_date,
                         number_of_guests, total_price, status, channel, created_at, checked_in_at, checked_out_at,
                         cancelled_at)
VALUES
    -- Mansion, checked out in November 2028
    (3001, 'REVENU0001', 3001, 3001, 3001, '2028-11-10', '2028-11-15', 2, 1000.00, 'CHECKED_OUT', 'ONLINE',
     '2028-10-01 10:00:00', '2028-11-10 15:00:00', '2028-11-15 10:00:00', NULL),
    -- Mansion, stay from November to December 2028: counts entirely for December, the month of check-out
    (3002, 'REVENU0002', 3001, 3001, 3001, '2028-11-28', '2028-12-02', 2, 1600.00, 'CHECKED_OUT', 'RECEPTION',
     '2028-10-01 10:00:00', '2028-11-28 15:00:00', '2028-12-02 09:00:00', NULL),
    -- Single Room, checked out late on 31 December 2028
    (3003, 'REVENU0003', 3001, 1001, 1001, '2028-12-30', '2028-12-31', 1, 120.50, 'CHECKED_OUT', 'RECEPTION',
     '2028-10-01 10:00:00', '2028-12-30 15:00:00', '2028-12-31 23:30:00', NULL),
    -- Mansion, checked out just before and just after the period
    (3004, 'REVENU0004', 3001, 3001, 3001, '2028-10-30', '2028-10-31', 2, 200.00, 'CHECKED_OUT', 'ONLINE',
     '2028-10-01 10:00:00', '2028-10-30 15:00:00', '2028-10-31 23:59:00', NULL),
    (3005, 'REVENU0005', 3001, 3001, 3001, '2028-12-31', '2029-01-01', 2, 200.00, 'CHECKED_OUT', 'ONLINE',
     '2028-10-01 10:00:00', '2028-12-31 15:00:00', '2029-01-01 00:00:00', NULL),
    -- Cancelled and No-Show reservations count no revenue
    (3006, 'REVENU0006', 3001, 3001, NULL, '2028-11-20', '2028-11-22', 2, 400.00, 'CANCELLED', 'ONLINE',
     '2028-10-01 10:00:00', NULL, NULL, '2028-11-01 10:00:00'),
    (3007, 'REVENU0007', 3001, 1002, NULL, '2028-11-20', '2028-11-21', 2, 150.00, 'NO_SHOW', 'ONLINE',
     '2028-10-01 10:00:00', NULL, NULL, NULL);
