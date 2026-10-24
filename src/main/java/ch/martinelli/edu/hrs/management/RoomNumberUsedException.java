package ch.martinelli.edu.hrs.management;

/**
 * Another room already has the entered room number (UC-017 A2, BR-001).
 */
public class RoomNumberUsedException extends RuntimeException {

    public RoomNumberUsedException(String roomNumber) {
        super("The room number " + roomNumber + " is already used");
    }
}
