package ch.martinelli.edu.hrs.roomcatalog;

public class RoomTypeNotFoundException extends RuntimeException {

    public RoomTypeNotFoundException() {
        super("Room type no longer exists");
    }
}
