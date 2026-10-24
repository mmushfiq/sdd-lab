package ch.martinelli.edu.hrs.reception;

import ch.martinelli.edu.hrs.reservation.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A reservation departing today, or an overdue one (UC-008 step 3 and A2).
 */
public record Departure(String reservationCode, String guestFirstName, String guestLastName, String roomNumber,
                        BigDecimal totalPrice, LocalDate departureDate, ReservationStatus status, boolean overdue) {

    public String guestName() {
        return guestFirstName + " " + guestLastName;
    }

    // UC-008 BR-002: Checked-out departures are marked as done.
    public boolean done() {
        return status == ReservationStatus.CHECKED_OUT;
    }
}
