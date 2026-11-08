package ch.martinelli.edu.hrs.reservation;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * What a guest sees of their own reservation online (US7/AC1); no other guest data (FR-024).
 */
public record GuestReservation(String reservationNumber, LocalDate arrival, LocalDate departure,
                               String roomTypeName, BigDecimal totalPrice, ReservationStatus status,
                               boolean cancellable) {
}
