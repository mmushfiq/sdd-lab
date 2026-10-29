package ch.martinelli.edu.hrs.guestbooking;

import ch.martinelli.edu.hrs.reservation.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * A reservation as the guest sees it: no room number, no guest data.
 */
public record GuestReservation(String bookingNumber, LocalDate arrival, LocalDate departure, String roomTypeName,
                               int numberOfGuests, BigDecimal totalPrice, ReservationStatus status) {

    static final ZoneId HOTEL_ZONE = ZoneId.of("Europe/Zurich");
    static final LocalTime CHECK_IN = LocalTime.of(14, 0);

    /**
     * Check-in time on the day before arrival, hotel time.
     */
    public Instant cancellationDeadline() {
        return arrival.minusDays(1).atTime(CHECK_IN).atZone(HOTEL_ZONE).toInstant();
    }

    public boolean beforeCancellationDeadline(Instant now) {
        return now.isBefore(cancellationDeadline());
    }
}
