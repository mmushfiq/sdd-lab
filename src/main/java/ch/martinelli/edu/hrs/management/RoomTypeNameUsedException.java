package ch.martinelli.edu.hrs.management;

/**
 * Another room type already has the entered name (UC-016 A2, BR-001).
 */
public class RoomTypeNameUsedException extends RuntimeException {

    public RoomTypeNameUsedException(String name) {
        super("The name " + name + " is already used");
    }
}
