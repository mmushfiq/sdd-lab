package ch.martinelli.edu.hrs.reservation;

import java.time.LocalDate;

/**
 * Criteria for the reservation list; null or blank values are not applied.
 */
public record ReservationFilter(String bookingNumber, String guestName, LocalDate from, LocalDate to,
                                boolean includeCancelled) {
}
