package ch.martinelli.edu.hrs.reservation;

/**
 * Thrown when too many lookups failed from the guest's network address (UC-002 A3, UC-003 A3).
 */
public class ReservationLookupLockedException extends RuntimeException {

    public ReservationLookupLockedException() {
        super("The reservation lookup is locked for this network address");
    }
}
