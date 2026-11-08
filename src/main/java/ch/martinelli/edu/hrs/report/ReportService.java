package ch.martinelli.edu.hrs.report;

import ch.martinelli.edu.hrs.reservation.BookingSource;
import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION_NIGHT;
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

        // 003 FR-009: the month's nights of checked-in and checked-out stays at their saved rates
        var occupied = dsl.select(DSL.count(), DSL.sum(RESERVATION_NIGHT.NIGHTLY_RATE))
                .from(RESERVATION_NIGHT).join(RESERVATION).on(RESERVATION.ID.eq(RESERVATION_NIGHT.RESERVATION_ID))
                .where(RESERVATION.STATUS.in(ReservationStatus.CHECKED_IN.name(), ReservationStatus.CHECKED_OUT.name()))
                .and(RESERVATION_NIGHT.NIGHT_DATE.ge(first))
                .and(RESERVATION_NIGHT.NIGHT_DATE.lt(next))
                .fetchSingle();
        var nights = occupied.value1();
        var revenue = occupied.value2() == null ? BigDecimal.ZERO : occupied.value2();

        var created = RESERVATION.CREATED_AT.ge(first.atStartOfDay()).and(RESERVATION.CREATED_AT.lt(next.atStartOfDay()));
        var createdCount = dsl.fetchCount(RESERVATION, created);
        var onlineCount = dsl.fetchCount(RESERVATION, created.and(RESERVATION.SOURCE.eq(BookingSource.ONLINE.name())));

        return new MonthlyReport(nights, dsl.fetchCount(ROOM) * month.lengthOfMonth(), revenue,
                createdCount, onlineCount);
    }
}
