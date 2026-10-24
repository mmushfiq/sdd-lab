package ch.martinelli.edu.hrs.staff;

/**
 * A staff member as listed and edited by the manager (UC-021 steps 2 and 3); the password is never part of it.
 */
public record StaffMemberOverview(Long id, String username, String firstName, String lastName, StaffRole role,
                                  boolean active) {
}
