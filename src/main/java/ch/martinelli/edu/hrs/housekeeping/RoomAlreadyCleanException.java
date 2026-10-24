package ch.martinelli.edu.hrs.housekeeping;

/**
 * Another housekeeper marked the room clean since it was displayed (UC-014 A1).
 */
public class RoomAlreadyCleanException extends RuntimeException {

    public RoomAlreadyCleanException() {
        super("The room is already clean");
    }
}
