package ch.martinelli.edu.hrs.reservation;

import java.time.LocalDate;

/**
 * The new values a receptionist enters for a reservation (UC-005 step 3). For a checked-in guest only the departure
 * date may differ from the reservation.
 */
public record ReservationChange(LocalDate arrivalDate, LocalDate departureDate, Long roomTypeId,
                                int numberOfGuests) {
}
