package ch.martinelli.edu.hrs.reservation;

import java.time.LocalDate;

public record BookingRequest(long roomId, LocalDate arrival, LocalDate departure, int numberOfGuests,
                             String guestName, String guestEmail, String guestPhone, BookingSource source) {
}
