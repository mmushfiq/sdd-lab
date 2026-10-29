package ch.martinelli.edu.hrs.reservation;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A reservation as shown in the list, with the lifecycle rules the list actions and the service share.
 */
public record ReservationListItem(String bookingNumber, String guestName, String guestEmail, String guestPhone,
                                  long roomId, String roomNumber, LocalDate arrival, LocalDate departure,
                                  int numberOfGuests, BigDecimal totalPrice, ReservationStatus status,
                                  boolean anonymized) {

    public StayPeriod period() {
        return new StayPeriod(arrival, departure);
    }

    public Guest guest() {
        return new Guest(guestName, guestEmail, guestPhone);
    }

    /**
     * Confirmed, not anonymized and not departed yet; a guest on the departure day can still extend.
     */
    public boolean changeable() {
        return status == ReservationStatus.CONFIRMED && !anonymized && !departure.isBefore(LocalDate.now());
    }

    /**
     * Reception may cancel any confirmed reservation, also during the stay.
     */
    public boolean cancellable() {
        return status == ReservationStatus.CONFIRMED;
    }

    /**
     * Cancelled or departed, and not anonymized yet.
     */
    public boolean anonymizable() {
        return !anonymized && (status == ReservationStatus.CANCELLED || departure.isBefore(LocalDate.now()));
    }
}
