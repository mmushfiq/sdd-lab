package ch.martinelli.edu.hrs.reservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * What a guest sees of their own reservation online (US7/AC1); no other guest data (FR-024).
 */
public record GuestReservation(String reservationNumber, LocalDate arrival, LocalDate departure,
                               String roomTypeName, BigDecimal totalPrice, List<NightRate> nights,
                               ReservationStatus status,
                               boolean cancellable, CancellationDeadline cancellationDeadline) {
}
