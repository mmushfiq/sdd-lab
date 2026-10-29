package ch.martinelli.edu.hrs.reservation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationListItem(String bookingNumber, String guestName, String roomNumber, LocalDate arrival,
                                  LocalDate departure, int numberOfGuests, BigDecimal totalPrice,
                                  ReservationStatus status) {
}
