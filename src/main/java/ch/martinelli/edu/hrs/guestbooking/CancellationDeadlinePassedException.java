package ch.martinelli.edu.hrs.guestbooking;

public class CancellationDeadlinePassedException extends RuntimeException {

    public static final String MESSAGE = "Online cancellation is no longer possible. Please call the hotel.";

    public CancellationDeadlinePassedException() {
        super(MESSAGE);
    }
}
