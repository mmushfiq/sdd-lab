package ch.martinelli.edu.hrs.staff;

/**
 * The values the manager enters for a staff member (UC-021 step 4). {@code password} is the initial password of a
 * new staff member or the new password of an existing one (A4); null or empty keeps the current password.
 */
public record StaffMemberData(String username, String firstName, String lastName, StaffRole role, boolean active,
                              String password) {

    @Override
    public String toString() {
        // UC-021 BR-003: The password is never shown or logged.
        return "StaffMemberData[username=%s, firstName=%s, lastName=%s, role=%s, active=%s]"
                .formatted(username, firstName, lastName, role, active);
    }
}
