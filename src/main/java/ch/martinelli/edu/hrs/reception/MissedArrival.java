package ch.martinelli.edu.hrs.reception;

import java.time.LocalDate;

/**
 * A Confirmed reservation whose arrival date is before today (UC-012 step 2).
 */
public record MissedArrival(String reservationCode, String guestFirstName, String guestLastName, String guestPhone,
                            LocalDate arrivalDate, LocalDate departureDate, String roomTypeName) {

    public String guestName() {
        return guestFirstName + " " + guestLastName;
    }
}
