-- Test data for UC-022 Log In and UC-023 Log Out. Every test staff member has the test password Secret-123
-- (bcrypt, cost factor 10).

INSERT INTO staff_member (id, username, password_hash, first_name, last_name, role, active)
VALUES (1001, 'rita', '$2a$10$4FqE45rVIyp98An7WePq8uM3Kel8JiE92YBuSr/8i46mEHDkZkdaS', 'Rita', 'Reception',
        'RECEPTIONIST', TRUE),
       (1002, 'mark', '$2a$10$4FqE45rVIyp98An7WePq8uM3Kel8JiE92YBuSr/8i46mEHDkZkdaS', 'Mark', 'Manager',
        'MANAGER', TRUE),
       (1003, 'hanna', '$2a$10$4FqE45rVIyp98An7WePq8uM3Kel8JiE92YBuSr/8i46mEHDkZkdaS', 'Hanna', 'Housekeeping',
        'HOUSEKEEPER', TRUE),
       -- An inactive staff member with the correct password
       (1004, 'ina', '$2a$10$4FqE45rVIyp98An7WePq8uM3Kel8JiE92YBuSr/8i46mEHDkZkdaS', 'Ina', 'Inactive',
        'RECEPTIONIST', FALSE);
