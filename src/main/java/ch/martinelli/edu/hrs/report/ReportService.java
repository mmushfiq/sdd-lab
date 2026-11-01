package ch.martinelli.edu.hrs.report;

import ch.martinelli.edu.hrs.frontdesk.FrontDeskService;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static org.jooq.impl.DSL.min;

/**
 * Monthly occupancy and revenue, computed live from the stays that were checked in.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final DSLContext ctx;

    public ReportService(DSLContext ctx) {
        this.ctx = ctx;
    }

    public record MonthFigures(YearMonth month, int roomNights, BigDecimal occupancyPercent, BigDecimal revenue) {
    }

    /**
     * {@code total.month} is January of the year; only its year is meaningful.
     */
    public record YearReport(int year, List<MonthFigures> months, MonthFigures total) {
    }

    public YearReport yearReport(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = start.plusYears(1);
        int[] nights = new int[12];
        BigDecimal[] revenue = new BigDecimal[12];
        Arrays.fill(revenue, BigDecimal.ZERO);

        ctx.selectFrom(RESERVATION)
                .where(RESERVATION.STATUS.in(FrontDeskService.CHECKED_IN, FrontDeskService.CHECKED_OUT))
                .and(RESERVATION.ARRIVAL_DATE.lt(end))
                .and(RESERVATION.DEPARTURE_DATE.gt(start))
                .forEach(r -> {
                    for (LocalDate night = r.getArrivalDate(); night.isBefore(r.getDepartureDate());
                         night = night.plusDays(1)) {
                        if (night.getYear() == year) {
                            int m = night.getMonthValue() - 1;
                            nights[m]++;
                            revenue[m] = revenue[m].add(r.getNightlyRate());
                        }
                    }
                });

        int rooms = ctx.fetchCount(ROOM);
        List<MonthFigures> months = new ArrayList<>();
        int totalNights = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (int m = 0; m < 12; m++) {
            YearMonth month = YearMonth.of(year, m + 1);
            months.add(new MonthFigures(month, nights[m], percent(nights[m], rooms * month.lengthOfMonth()),
                    revenue[m].setScale(2, RoundingMode.HALF_UP)));
            totalNights += nights[m];
            totalRevenue = totalRevenue.add(revenue[m]);
        }
        MonthFigures total = new MonthFigures(YearMonth.of(year, 1), totalNights,
                percent(totalNights, rooms * Year.of(year).length()), totalRevenue.setScale(2, RoundingMode.HALF_UP));
        return new YearReport(year, months, total);
    }

    /**
     * Year of the first reservation, or empty when there is none.
     */
    public Optional<Integer> firstYear() {
        return Optional.ofNullable(ctx.select(min(RESERVATION.ARRIVAL_DATE)).from(RESERVATION).fetchOne(0, LocalDate.class))
                .map(LocalDate::getYear);
    }

    private static BigDecimal percent(int nights, int capacity) {
        if (capacity == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return BigDecimal.valueOf(nights).multiply(HUNDRED).divide(BigDecimal.valueOf(capacity), 1,
                RoundingMode.HALF_UP);
    }
}
