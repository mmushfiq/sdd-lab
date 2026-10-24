package ch.martinelli.edu.hrs.reservation;

import java.time.LocalDate;

/**
 * What a guest confirms in the online reservation: the searched dates and number of guests, the selected room type,
 * and the guest details.
 */
public record OnlineReservationRequest(LocalDate arrivalDate, LocalDate departureDate, int numberOfGuests,
                                       Long roomTypeId, GuestDetails guestDetails) {
}
