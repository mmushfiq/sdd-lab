package ch.martinelli.edu.hrs.reservation;

/**
 * The reservation's state no longer allows the requested change, cancellation or anonymization.
 */
public class ReservationNotModifiableException extends RuntimeException {

    public ReservationNotModifiableException(String message) {
        super(message);
    }
}
