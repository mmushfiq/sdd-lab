package ch.martinelli.edu.hrs.report;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UC-020 View Revenue Report: the manager sees the revenue per month and per room type for a chosen period.
 * No data is changed.
 */
@Service
public class RevenueReportService {

    private final RevenueReportRepository repository;
    private final Clock clock;

    public RevenueReportService(RevenueReportRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /**
     * Steps 3 to 5, A2.
     *
     * @throws InvalidReportPeriodException when the period breaks BR-004 (A1)
     */
    public RevenueReport report(LocalDate firstDate, LocalDate lastDate) {
        // UC-020 BR-004: The period is at most 366 days long and ends today or earlier.
        ReportPeriod.check(firstDate, lastDate, today());
        List<RoomTypeMonthRevenue> revenues = repository.findRevenue(firstDate, lastDate);

        // UC-020 BR-003: Every room type with revenue has a column, including inactive room types; the active room
        // types have a column even without revenue (A2).
        Map<Long, RevenueRoomType> roomTypes = new HashMap<>();
        repository.findActiveRoomTypes().forEach(roomType -> roomTypes.put(roomType.id(), roomType));
        revenues.forEach(revenue -> roomTypes.putIfAbsent(revenue.roomTypeId(),
                new RevenueRoomType(revenue.roomTypeId(), revenue.roomTypeName())));
        List<RevenueRoomType> columns = roomTypes.values().stream()
                .sorted(Comparator.comparing(RevenueRoomType::name))
                .toList();

        // A2: every month of the period has a row, with 0.00 when nothing was checked out
        Map<YearMonth, Map<Long, BigDecimal>> perMonth = new LinkedHashMap<>();
        for (YearMonth month = YearMonth.from(firstDate); !month.isAfter(YearMonth.from(lastDate));
             month = month.plusMonths(1)) {
            perMonth.put(month, new HashMap<>());
        }
        Map<Long, BigDecimal> perRoomType = new HashMap<>();
        BigDecimal total = RevenueReport.ZERO;
        for (RoomTypeMonthRevenue revenue : revenues) {
            // UC-020 BR-002: The total price counts entirely for the month of check-out.
            YearMonth month = YearMonth.of(revenue.year(), revenue.month());
            perMonth.get(month).merge(revenue.roomTypeId(), revenue.revenue(), BigDecimal::add);
            perRoomType.merge(revenue.roomTypeId(), revenue.revenue(), BigDecimal::add);
            total = total.add(revenue.revenue());
        }

        List<MonthlyRevenue> months = new ArrayList<>();
        perMonth.forEach((month, revenue) -> months.add(new MonthlyRevenue(month, revenue,
                revenue.values().stream().reduce(RevenueReport.ZERO, BigDecimal::add))));
        return new RevenueReport(firstDate, lastDate, columns, months, perRoomType, total);
    }
}
