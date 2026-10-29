package ch.martinelli.edu.hrs.roomcatalog;

public class RoomTypeNameExistsException extends RuntimeException {

    public RoomTypeNameExistsException() {
        super("Name already exists");
    }
}
