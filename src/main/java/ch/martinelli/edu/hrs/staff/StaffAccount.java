package ch.martinelli.edu.hrs.staff;

/**
 * The login data of an active staff member (UC-022 step 3).
 */
public record StaffAccount(String username, String passwordHash, StaffRole role) {
}
