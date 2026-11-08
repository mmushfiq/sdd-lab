package ch.martinelli.edu.hrs.reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The last time a guest may cancel a reservation online (002 FR-002 to FR-005): check-in time on the arrival
 * date minus the reservation's hours, as wall-clock time in Europe/Zurich (research R2).
 */
public record CancellationDeadline(LocalDateTime deadline) {

    public static final ZoneId ZONE = ZoneId.of("Europe/Zurich");
    static final LocalTime CHECK_IN = LocalTime.of(15, 0);
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.ENGLISH);

    public static CancellationDeadline of(LocalDate arrival, int hoursBefore) {
        return new CancellationDeadline(arrival.atTime(CHECK_IN).minusHours(hoursBefore));
    }

    /**
     * Exclusive: at the deadline itself online cancellation is no longer possible.
     */
    public boolean allowsOnlineCancellationAt(LocalDateTime now) {
        return now.isBefore(deadline);
    }

    public String text() {
        return "Free online cancellation until " + FORMAT.format(deadline) + " (Zurich time)";
    }

    public String passedText() {
        return "Online cancellation was possible until " + FORMAT.format(deadline)
                + " (Zurich time). Please call reception to cancel.";
    }
}
