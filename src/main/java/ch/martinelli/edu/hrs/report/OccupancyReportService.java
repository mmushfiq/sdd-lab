package ch.martinelli.edu.hrs.report;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UC-019 View Occupancy Report: the manager sees the occupancy rate per day, per month and for a chosen period.
 * No data is changed.
 */
@Service
public class OccupancyReportService {

    public static final int MAX_PERIOD_DAYS = ReportPeriod.MAX_DAYS;

    private final OccupancyReportRepository repository;
    private final Clock clock;

    public OccupancyReportService(OccupancyReportRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /**
     * Steps 3 to 5.
     *
     * @throws InvalidReportPeriodException when the period breaks BR-004 (A1)
     */
    public OccupancyReport report(LocalDate firstDate, LocalDate lastDate) {
        checkPeriod(firstDate, lastDate);
        List<DailyOccupancy> days = repository.findDailyOccupancy(firstDate, lastDate);

        // UC-019 BR-004: A month that is only partly in the period counts only its days in the period.
        Map<YearMonth, OccupancyFigures> months = new LinkedHashMap<>();
        OccupancyFigures period = new OccupancyFigures(0, 0);
        for (DailyOccupancy day : days) {
            // UC-019 BR-001: Months and the period are summed from room nights, not averaged from daily rates.
            months.merge(YearMonth.from(day.day()), day.figures(), OccupancyFigures::plus);
            period = period.plus(day.figures());
        }
        List<MonthlyOccupancy> monthly = months.entrySet().stream()
                .map(entry -> new MonthlyOccupancy(entry.getKey(), entry.getValue()))
                .toList();
        return new OccupancyReport(firstDate, lastDate, days, monthly, period);
    }

    /**
     * Step 3.
     */
    // UC-019 BR-004: The period is at most 366 days long and ends today or earlier.
    private void checkPeriod(LocalDate firstDate, LocalDate lastDate) {
        ReportPeriod.check(firstDate, lastDate, today());
    }

    public static long periodDays(LocalDate firstDate, LocalDate lastDate) {
        return ReportPeriod.days(firstDate, lastDate);
    }
}
