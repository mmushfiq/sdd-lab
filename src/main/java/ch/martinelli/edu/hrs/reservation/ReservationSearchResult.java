package ch.martinelli.edu.hrs.reservation;

import java.time.LocalDate;

/**
 * A reservation in the result list of the reservation search (UC-007 step 3).
 */
public record ReservationSearchResult(String reservationCode, String guestFirstName, String guestLastName,
                                      LocalDate arrivalDate, LocalDate departureDate, String roomTypeName,
                                      int numberOfGuests, ReservationStatus status) {

    public String guestName() {
        return guestFirstName + " " + guestLastName;
    }
}
