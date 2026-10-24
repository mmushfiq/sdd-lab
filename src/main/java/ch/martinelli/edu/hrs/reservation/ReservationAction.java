package ch.martinelli.edu.hrs.reservation;

import java.util.List;

/**
 * What a receptionist can do with a reservation, depending on its status (UC-007 step 5).
 */
public enum ReservationAction {
    CHANGE, CANCEL, CHECK_IN, CHECK_OUT, RECORD_NO_SHOW;

    /**
     * The actions the status of a reservation allows.
     */
    // UC-005 BR-001: Confirmed and Checked In reservations can be changed.
    // UC-006 BR-001: Confirmed reservations can be cancelled by the receptionist.
    // UC-010 BR-001: Only Checked In reservations can be checked out.
    // UC-012 BR-001: A no-show can be recorded only for a Confirmed reservation.
    public static List<ReservationAction> allowedFor(ReservationStatus status) {
        return switch (status) {
            case CONFIRMED -> List.of(CHANGE, CANCEL, CHECK_IN, RECORD_NO_SHOW);
            case CHECKED_IN -> List.of(CHANGE, CHECK_OUT);
            case CHECKED_OUT, CANCELLED, NO_SHOW -> List.of();
        };
    }
}
