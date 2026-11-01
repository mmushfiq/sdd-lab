package ch.martinelli.edu.hrs.core.security;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

/**
 * A logged-in staff user with exactly one role (RECEPTIONIST, MANAGER or HOUSEKEEPING).
 */
public class StaffUser extends User {

    private final String fullName;
    private final String role;

    public StaffUser(String username, String passwordHash, String fullName, String role) {
        super(username, passwordHash, AuthorityUtils.createAuthorityList("ROLE_" + role));
        this.fullName = fullName;
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    /**
     * The role as shown to users, e.g. "Receptionist".
     */
    public String getRoleName() {
        return role.charAt(0) + role.substring(1).toLowerCase();
    }
}
