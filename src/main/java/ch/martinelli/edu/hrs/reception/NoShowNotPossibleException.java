package ch.martinelli.edu.hrs.reception;

import ch.martinelli.edu.hrs.reservation.ReservationStatus;

/**
 * A no-show cannot be recorded for the reservation (UC-012 A4, BR-001).
 */
public class NoShowNotPossibleException extends RuntimeException {

    private final ReservationStatus status;

    public NoShowNotPossibleException(ReservationStatus status) {
        super("A no-show cannot be recorded for a reservation with status " + status);
        this.status = status;
    }

    public ReservationStatus status() {
        return status;
    }
}
