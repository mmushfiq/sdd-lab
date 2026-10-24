package ch.martinelli.edu.hrs.reservation;

/**
 * Thrown when a guest may not cancel a reservation online (UC-003 A4, A5 and A7) or the receptionist may not
 * cancel it (UC-006 A1 and A3).
 */
public class ReservationNotCancellableException extends RuntimeException {

    public enum Reason {
        /** The reservation does not have status Confirmed (A4). */
        NOT_CONFIRMED,
        /** Less than 24 hours remain until the check-in time on the arrival date (A5). */
        DEADLINE_PASSED
    }

    private final Reason reason;

    public ReservationNotCancellableException(Reason reason) {
        super("The reservation cannot be cancelled online: " + reason);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
