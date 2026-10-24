package ch.martinelli.edu.hrs.reservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * All data of a reservation as the receptionist sees it (UC-007 step 5), with the guest's contact data and the
 * assigned room number when a room is assigned.
 */
public record ReservationDetail(String reservationCode, ReservationStatus status, LocalDate arrivalDate,
                                LocalDate departureDate, String roomTypeName, int numberOfGuests,
                                BigDecimal totalPrice, ReservationChannel channel, LocalDateTime createdAt,
                                LocalDateTime checkedInAt, LocalDateTime checkedOutAt, LocalDateTime cancelledAt,
                                String guestFirstName, String guestLastName, String guestEmail, String guestPhone,
                                String roomNumber) {
}
