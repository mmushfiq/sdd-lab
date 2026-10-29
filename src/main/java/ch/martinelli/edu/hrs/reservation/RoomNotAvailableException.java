package ch.martinelli.edu.hrs.reservation;

public class RoomNotAvailableException extends RuntimeException {

    public static final String NO_LONGER_AVAILABLE = "Room is no longer available for these dates";
    public static final String NOT_AVAILABLE = "Room is not available for these dates";

    public RoomNotAvailableException() {
        this(NO_LONGER_AVAILABLE);
    }

    public RoomNotAvailableException(String message) {
        super(message);
    }
}
