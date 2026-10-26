package ch.martinelli.edu.hrs.reservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A reservation as the guest sees it after a successful lookup (UC-002 step 4), with its stored online
 * cancellation deadline (UC-003 BR-001).
 */
public record OwnReservation(String reservationCode, ReservationStatus status, LocalDate arrivalDate,
                             LocalDate departureDate, String roomTypeName, int numberOfGuests,
                             BigDecimal totalPrice, LocalDateTime onlineCancellationDeadline) {

    public long nights() {
        return OnlineReservationService.nights(arrivalDate, departureDate);
    }
}
