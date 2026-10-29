package ch.martinelli.edu.hrs.roomcatalog;

public class RoomNumberExistsException extends RuntimeException {

    public RoomNumberExistsException() {
        super("Room number already exists");
    }
}
