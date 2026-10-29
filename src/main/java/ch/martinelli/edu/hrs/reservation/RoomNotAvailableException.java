package ch.martinelli.edu.hrs.reservation;

public class RoomNotAvailableException extends RuntimeException {

    public RoomNotAvailableException() {
        super("Room is no longer available for these dates");
    }
}
