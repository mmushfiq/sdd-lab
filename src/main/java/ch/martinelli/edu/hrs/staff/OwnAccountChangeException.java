package ch.martinelli.edu.hrs.staff;

/**
 * The manager tried to deactivate their own account or change their own role (UC-021 A3).
 */
public class OwnAccountChangeException extends RuntimeException {

    public OwnAccountChangeException() {
        super("A manager cannot deactivate their own account or change their own role");
    }
}
