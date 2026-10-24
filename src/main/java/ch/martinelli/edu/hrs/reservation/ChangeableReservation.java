package ch.martinelli.edu.hrs.reservation;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A reservation as the receptionist changes it (UC-005 step 2), with the assigned room of a checked-in guest.
 */
public record ChangeableReservation(Long id, String reservationCode, ReservationStatus status, LocalDate arrivalDate,
                                    LocalDate departureDate, Long roomTypeId, String roomTypeName,
                                    int numberOfGuests, BigDecimal totalPrice, Long roomId, String roomNumber,
                                    String guestEmail) {

    public long nights() {
        return OnlineReservationService.nights(arrivalDate, departureDate);
    }
}
