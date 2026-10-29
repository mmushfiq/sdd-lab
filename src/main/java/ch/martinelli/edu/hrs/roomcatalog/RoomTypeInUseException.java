package ch.martinelli.edu.hrs.roomcatalog;

public class RoomTypeInUseException extends RuntimeException {

    public RoomTypeInUseException() {
        super("Room type is used by rooms and cannot be deleted");
    }
}
