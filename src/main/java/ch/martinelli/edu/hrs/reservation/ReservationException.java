package ch.martinelli.edu.hrs.reservation;

/**
 * A reservation rule is broken; the message is meant for the user.
 */
public class ReservationException extends RuntimeException {

    public ReservationException(String message) {
        super(message);
    }
}
