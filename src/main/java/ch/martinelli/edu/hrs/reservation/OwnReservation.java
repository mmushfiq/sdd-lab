package ch.martinelli.edu.hrs.reservation;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A reservation as the guest sees it after a successful lookup (UC-002 step 4).
 */
public record OwnReservation(String reservationCode, ReservationStatus status, LocalDate arrivalDate,
                             LocalDate departureDate, String roomTypeName, int numberOfGuests,
                             BigDecimal totalPrice) {

    public long nights() {
        return OnlineReservationService.nights(arrivalDate, departureDate);
    }
}
