package ch.martinelli.edu.hrs.report;

import ch.martinelli.edu.hrs.reservation.BookingSource;
import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final DSLContext dsl;

    public ReportService(DSLContext dsl) {
        this.dsl = dsl;
    }

    public MonthlyReport monthlyReport(YearMonth month) {
        var first = month.atDay(1);
        var next = month.plusMonths(1).atDay(1);

        var nights = 0;
        var revenue = BigDecimal.ZERO;
        var stays = dsl.selectFrom(RESERVATION)
                .where(RESERVATION.STATUS.in(ReservationStatus.CHECKED_IN.name(), ReservationStatus.CHECKED_OUT.name()))
                .and(RESERVATION.ARRIVAL_DATE.lt(next))
                .and(RESERVATION.DEPARTURE_DATE.gt(first))
                .fetch();
        for (var stay : stays) {
            var from = stay.getArrivalDate().isAfter(first) ? stay.getArrivalDate() : first;
            var to = stay.getDepartureDate().isBefore(next) ? stay.getDepartureDate() : next;
            var inMonth = (int) ChronoUnit.DAYS.between(from, to);
            nights += inMonth;
            revenue = revenue.add(stay.getNightlyRate().multiply(BigDecimal.valueOf(inMonth)));
        }

        var created = RESERVATION.CREATED_AT.ge(first.atStartOfDay()).and(RESERVATION.CREATED_AT.lt(next.atStartOfDay()));
        var createdCount = dsl.fetchCount(RESERVATION, created);
        var onlineCount = dsl.fetchCount(RESERVATION, created.and(RESERVATION.SOURCE.eq(BookingSource.ONLINE.name())));

        return new MonthlyReport(nights, dsl.fetchCount(ROOM) * month.lengthOfMonth(), revenue,
                createdCount, onlineCount);
    }
}
