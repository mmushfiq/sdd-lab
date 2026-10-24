package ch.martinelli.edu.hrs.reservation;

/**
 * The reservation has a status that does not allow the change (UC-005 A1, BR-001).
 */
public class ReservationNotChangeableException extends RuntimeException {

    private final ReservationStatus status;

    public ReservationNotChangeableException(ReservationStatus status) {
        super("A reservation with status " + status + " cannot be changed");
        this.status = status;
    }

    public ReservationStatus status() {
        return status;
    }
}
