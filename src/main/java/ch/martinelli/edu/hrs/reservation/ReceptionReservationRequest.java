package ch.martinelli.edu.hrs.reservation;

import java.time.LocalDate;

/**
 * What a receptionist confirms for a phone or walk-in guest: the dates and number of guests, the selected room type,
 * and the guest details (UC-004 step 8).
 */
public record ReceptionReservationRequest(LocalDate arrivalDate, LocalDate departureDate, int numberOfGuests,
                                          Long roomTypeId, GuestDetails guestDetails) {
}
