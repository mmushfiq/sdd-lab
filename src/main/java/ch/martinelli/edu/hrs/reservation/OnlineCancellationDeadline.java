package ch.martinelli.edu.hrs.reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The online cancellation deadline of a reservation: the moment from which the guest can no longer cancel it online
 * (UC-003 BR-001). It is stored per reservation, in the hotel's local time.
 */
public final class OnlineCancellationDeadline {

    public static final LocalTime CHECK_IN_TIME = LocalTime.of(14, 0);
    public static final int ONLINE_CANCELLATION_HOURS = 48;

    public static final String NO_LONGER_POSSIBLE = "This reservation can no longer be cancelled online. "
            + "The hotel's reception can still cancel it.";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm",
            Locale.ENGLISH);

    private OnlineCancellationDeadline() {
    }

    /**
     * The deadline for a reservation recorded now, or whose arrival date is changed now.
     */
    public static LocalDateTime forArrival(LocalDate arrivalDate) {
        // UC-003 BR-001: 48 hours before the check-in time (14:00) on the arrival date, in the hotel's local time.
        return arrivalDate.atTime(CHECK_IN_TIME).minusHours(ONLINE_CANCELLATION_HOURS);
    }

    /**
     * Whether a guest can still cancel online at {@code now}.
     */
    public static boolean notPassed(LocalDateTime deadline, LocalDateTime now) {
        // UC-003 BR-001: A guest can cancel online until the stored online cancellation deadline of the reservation.
        return now.isBefore(deadline);
    }

    /**
     * The text shown to the guest for a Confirmed reservation.
     */
    public static String describe(LocalDateTime deadline, LocalDateTime now) {
        // UC-003 BR-006: The deadline as a date and time in local time, or that online cancellation is no longer
        // possible and reception can still cancel.
        return notPassed(deadline, now)
                ? "Free online cancellation before " + FORMATTER.format(deadline)
                : NO_LONGER_POSSIBLE;
    }
}
