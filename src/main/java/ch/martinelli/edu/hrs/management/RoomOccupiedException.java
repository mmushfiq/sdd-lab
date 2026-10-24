package ch.martinelli.edu.hrs.management;

/**
 * An occupied room can be deactivated or moved to another room type only after check-out (UC-017 A4).
 */
public class RoomOccupiedException extends RuntimeException {

    public RoomOccupiedException() {
        super("The room is occupied");
    }
}
