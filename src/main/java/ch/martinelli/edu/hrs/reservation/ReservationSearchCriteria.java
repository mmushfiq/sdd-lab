package ch.martinelli.edu.hrs.reservation;

import java.time.LocalDate;

/**
 * The criteria a receptionist enters in the reservation search (UC-007 step 2); every criterion is optional.
 */
public record ReservationSearchCriteria(String guestName, String reservationCode, LocalDate date) {

    /**
     * True when no criterion is entered; text of only spaces counts as not entered (UC-007 A1).
     */
    public boolean isEmpty() {
        return isBlank(guestName) && isBlank(reservationCode) && date == null;
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
