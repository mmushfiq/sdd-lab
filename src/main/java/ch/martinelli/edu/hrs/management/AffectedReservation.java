package ch.martinelli.edu.hrs.management;

/**
 * A Confirmed or Checked In reservation with more guests than a new maximum (UC-016 A3).
 */
public record AffectedReservation(String reservationCode, int numberOfGuests) {
}
