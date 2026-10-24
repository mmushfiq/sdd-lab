-- Test data for UC-021 Maintain Staff Members. The staff members take the password hash of mark (V103), so their
-- password is the test password Secret-123 as well. Tests restore these rows after every change.

INSERT INTO staff_member (id, username, password_hash, first_name, last_name, role, active)
SELECT 3101, 'sam', password_hash, 'Sam', 'Service', 'RECEPTIONIST', TRUE FROM staff_member WHERE id = 1002
UNION ALL
SELECT 3102, 'hugo', password_hash, 'Hugo', 'Housekeeper', 'HOUSEKEEPER', TRUE FROM staff_member WHERE id = 1002
UNION ALL
-- A manager who left the hotel
SELECT 3103, 'lea', password_hash, 'Lea', 'Left', 'MANAGER', FALSE FROM staff_member WHERE id = 1002;
