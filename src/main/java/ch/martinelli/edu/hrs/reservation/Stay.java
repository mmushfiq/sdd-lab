package ch.martinelli.edu.hrs.reservation;

import java.time.LocalDate;

/**
 * A reservation as the front desk lists it in today's arrivals and departures.
 */
public record Stay(long reservationId, String reservationNumber, String guestName, String roomNumber,
                   LocalDate arrival, LocalDate departure, int numberOfGuests) {
}
