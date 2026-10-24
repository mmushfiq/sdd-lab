package ch.martinelli.edu.hrs.reservation;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The result of a recorded online reservation. {@code confirmationEmailAccepted} is false when the Email Service did
 * not accept the confirmation email on the first attempt and it is being retried.
 */
public record ReservationConfirmation(String reservationCode, LocalDate arrivalDate, LocalDate departureDate,
                                      String roomTypeName, int numberOfGuests, long nights, BigDecimal totalPrice,
                                      boolean confirmationEmailAccepted) {
}
