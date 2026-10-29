package ch.martinelli.edu.hrs.reservation;

import org.jooq.Condition;
import org.jooq.Field;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * The nights from arrival to departure, as the half-open interval [arrival, departure).
 */
public record StayPeriod(LocalDate arrival, LocalDate departure) {

    public StayPeriod {
        Objects.requireNonNull(arrival, "arrival");
        Objects.requireNonNull(departure, "departure");
        if (!departure.isAfter(arrival)) {
            throw new IllegalArgumentException("Departure must be after arrival");
        }
    }

    public int nights() {
        return (int) ChronoUnit.DAYS.between(arrival, departure);
    }

    /**
     * The overlap rule as SQL: the stored stay shares at least one night with this one.
     * A departure on this arrival day does not overlap (back-to-back is allowed).
     */
    public Condition overlaps(Field<LocalDate> otherArrival, Field<LocalDate> otherDeparture) {
        return otherArrival.lt(departure).and(otherDeparture.gt(arrival));
    }
}
