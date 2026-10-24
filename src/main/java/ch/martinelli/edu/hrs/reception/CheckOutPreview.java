package ch.martinelli.edu.hrs.reception;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * What the receptionist sees before confirming a check-out (UC-010 step 4, A2 and A3): the reserved and the new
 * departure date and total price.
 */
public record CheckOutPreview(String reservationCode, String roomNumber, LocalDate arrivalDate,
                              LocalDate reservedDepartureDate, BigDecimal reservedTotalPrice,
                              LocalDate newDepartureDate, BigDecimal newTotalPrice, Kind kind) {

    public enum Kind {
        /** The departure date is today. */
        ON_TIME,
        /** The departure date is after today (A2). */
        EARLY,
        /** The departure date is before today (A3). */
        OVERDUE
    }

    public long nights() {
        return ChronoUnit.DAYS.between(arrivalDate, newDepartureDate);
    }
}
