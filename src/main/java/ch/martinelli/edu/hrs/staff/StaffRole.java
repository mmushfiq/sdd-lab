package ch.martinelli.edu.hrs.staff;

/**
 * Role of a staff member; the constant names are the values stored in STAFF_MEMBER.ROLE.
 */
public enum StaffRole {
    RECEPTIONIST, MANAGER, HOUSEKEEPER;

    /** Names of the roles as used in {@code @RolesAllowed}. */
    public static final String RECEPTIONIST_ROLE = "RECEPTIONIST";
    public static final String MANAGER_ROLE = "MANAGER";
    public static final String HOUSEKEEPER_ROLE = "HOUSEKEEPER";
}
