package ch.martinelli.edu.hrs.management;

import java.util.List;

/**
 * The new maximum number of guests is lower than the number of guests of these reservations (UC-016 A3, BR-003).
 */
public class MaxGuestsBelowReservationsException extends RuntimeException {

    private final List<AffectedReservation> reservations;

    public MaxGuestsBelowReservationsException(List<AffectedReservation> reservations) {
        super("Reservations with more guests exist: " + reservations);
        this.reservations = List.copyOf(reservations);
    }

    public List<AffectedReservation> reservations() {
        return reservations;
    }
}
