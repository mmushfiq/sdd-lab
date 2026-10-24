package ch.martinelli.edu.hrs.reception;

/**
 * The selected room was assigned to another guest, set to dirty or blocked since it was listed (UC-009 A4).
 */
public class RoomNoLongerAssignableException extends RuntimeException {

    public RoomNoLongerAssignableException() {
        super("The room can no longer be assigned");
    }
}
