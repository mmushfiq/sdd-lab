package ch.martinelli.edu.hrs.reception;

import ch.martinelli.edu.hrs.reservation.ReservationStatus;

/**
 * The reservation cannot be checked in (UC-009 A1 and A2).
 */
public class CheckInNotPossibleException extends RuntimeException {

    public enum Reason {
        /** The reservation does not have status Confirmed (A1). */
        NOT_CONFIRMED,
        /** Today is not the arrival date (A2). */
        NOT_ARRIVAL_DATE
    }

    private final Reason reason;
    private final ReservationStatus status;

    public CheckInNotPossibleException(Reason reason, ReservationStatus status) {
        super("The reservation cannot be checked in: " + reason);
        this.reason = reason;
        this.status = status;
    }

    public Reason reason() {
        return reason;
    }

    public ReservationStatus status() {
        return status;
    }
}
