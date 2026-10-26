package ch.martinelli.edu.hrs.reservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The result of a recorded reservation, with the online cancellation deadline it was recorded with. {@code confirmationEmailAccepted} is false when the Email Service did
 * not accept the confirmation email on the first attempt and it is being retried.
 */
public record ReservationConfirmation(String reservationCode, LocalDate arrivalDate, LocalDate departureDate,
                                      String roomTypeName, int numberOfGuests, long nights, BigDecimal totalPrice,
                                      LocalDateTime onlineCancellationDeadline, boolean confirmationEmailAccepted) {
}
