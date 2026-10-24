package ch.martinelli.edu.hrs.management;

import ch.martinelli.edu.hrs.reservation.ReservationStatus;

import java.time.LocalDate;

/**
 * A reservation of the guest shown before the erasure (UC-024 step 3, A2).
 */
public record GuestReservation(String reservationCode, LocalDate arrivalDate, LocalDate departureDate,
                               ReservationStatus status) {

    // UC-024 BR-002: Confirmed and Checked In reservations are open.
    public boolean open() {
        return status == ReservationStatus.CONFIRMED || status == ReservationStatus.CHECKED_IN;
    }
}
