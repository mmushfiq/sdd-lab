package ch.martinelli.edu.hrs.report;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * The period of a manager report (UC-019 BR-004, UC-020 BR-004).
 */
public final class ReportPeriod {

    public static final int MAX_DAYS = 366;

    private ReportPeriod() {
    }

    /**
     * @throws InvalidReportPeriodException when a date is missing, the last date is before the first date or after
     *                                      today, or the period is longer than 366 days
     */
    // UC-019 BR-004, UC-020 BR-004: The period is at most 366 days long and ends today or earlier.
    public static void check(LocalDate firstDate, LocalDate lastDate, LocalDate today) {
        if (firstDate == null || lastDate == null) {
            throw new InvalidReportPeriodException("Enter the first and the last date");
        }
        if (lastDate.isBefore(firstDate)) {
            throw new InvalidReportPeriodException("The last date must be on or after the first date");
        }
        if (lastDate.isAfter(today)) {
            throw new InvalidReportPeriodException("The last date must be today or earlier");
        }
        if (days(firstDate, lastDate) > MAX_DAYS) {
            throw new InvalidReportPeriodException("The period can be at most " + MAX_DAYS + " days long");
        }
    }

    public static long days(LocalDate firstDate, LocalDate lastDate) {
        return ChronoUnit.DAYS.between(firstDate, lastDate) + 1;
    }
}
