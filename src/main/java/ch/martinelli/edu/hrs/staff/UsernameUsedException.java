package ch.martinelli.edu.hrs.staff;

/**
 * Another staff member already has the username (UC-021 A2).
 */
public class UsernameUsedException extends RuntimeException {

    public UsernameUsedException(String username) {
        super("Username already used: " + username);
    }
}
