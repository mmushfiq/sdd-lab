package ch.martinelli.edu.hrs.reception;

import ch.martinelli.edu.hrs.reservation.ReservationStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * A reservation arriving today (UC-008 step 2).
 */
public record Arrival(String reservationCode, String guestFirstName, String guestLastName, String roomTypeName,
                      LocalDate arrivalDate, LocalDate departureDate, int numberOfGuests, ReservationStatus status) {

    public String guestName() {
        return guestFirstName + " " + guestLastName;
    }

    public long nights() {
        return ChronoUnit.DAYS.between(arrivalDate, departureDate);
    }

    // UC-008 BR-001: Checked-in arrivals are marked as done.
    public boolean done() {
        return status == ReservationStatus.CHECKED_IN;
    }
}
