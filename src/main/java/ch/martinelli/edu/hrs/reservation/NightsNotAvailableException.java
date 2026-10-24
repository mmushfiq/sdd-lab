package ch.martinelli.edu.hrs.reservation;

import java.time.LocalDate;
import java.util.List;

/**
 * The room type is not available, or the assigned room is blocked, on these nights (UC-005 A4 and A7).
 */
public class NightsNotAvailableException extends RuntimeException {

    private final List<LocalDate> nights;

    public NightsNotAvailableException(List<LocalDate> nights) {
        super("Not available for the nights " + nights);
        this.nights = List.copyOf(nights);
    }

    public List<LocalDate> nights() {
        return nights;
    }
}
