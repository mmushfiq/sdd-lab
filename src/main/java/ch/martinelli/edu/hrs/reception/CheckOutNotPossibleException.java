package ch.martinelli.edu.hrs.reception;

import ch.martinelli.edu.hrs.reservation.ReservationStatus;

/**
 * Only a checked-in guest can be checked out (UC-010 A1 and A5).
 */
public class CheckOutNotPossibleException extends RuntimeException {

    private final ReservationStatus status;

    public CheckOutNotPossibleException(ReservationStatus status) {
        super("A reservation with status " + status + " cannot be checked out");
        this.status = status;
    }

    public ReservationStatus status() {
        return status;
    }
}
