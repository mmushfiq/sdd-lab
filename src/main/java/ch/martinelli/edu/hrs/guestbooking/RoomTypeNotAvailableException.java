package ch.martinelli.edu.hrs.guestbooking;

public class RoomTypeNotAvailableException extends RuntimeException {

    public RoomTypeNotAvailableException() {
        super("This room type is no longer available for these dates");
    }
}
