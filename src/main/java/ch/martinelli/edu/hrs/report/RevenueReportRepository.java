package ch.martinelli.edu.hrs.report;

import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static org.jooq.impl.DSL.month;
import static org.jooq.impl.DSL.sum;
import static org.jooq.impl.DSL.year;

/**
 * Sums the revenue of a period per room type and month of check-out (UC-020 step 4). Only reads data.
 */
@Repository
public class RevenueReportRepository {

    private final DSLContext ctx;

    public RevenueReportRepository(DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * The revenue per room type and month of the reservations checked out from {@code firstDate} to
     * {@code lastDate}; room type and month combinations without revenue are left out.
     */
    public List<RoomTypeMonthRevenue> findRevenue(LocalDate firstDate, LocalDate lastDate) {
        var checkOutYear = year(RESERVATION.CHECKED_OUT_AT);
        var checkOutMonth = month(RESERVATION.CHECKED_OUT_AT);
        return ctx.select(ROOM_TYPE.ID, ROOM_TYPE.NAME, checkOutYear, checkOutMonth, sum(RESERVATION.TOTAL_PRICE))
                .from(RESERVATION)
                .join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(RESERVATION.ROOM_TYPE_ID))
                // UC-020 BR-001: Only Checked Out reservations count; Cancelled and No-Show reservations do not.
                .where(RESERVATION.STATUS.eq(ReservationStatus.CHECKED_OUT.name()))
                // UC-020 BR-001: The time of check-out lies in the period.
                .and(RESERVATION.CHECKED_OUT_AT.ge(firstDate.atStartOfDay()))
                .and(RESERVATION.CHECKED_OUT_AT.lt(lastDate.plusDays(1).atStartOfDay()))
                // UC-020 BR-002: The total price counts entirely for the month of check-out.
                .groupBy(ROOM_TYPE.ID, ROOM_TYPE.NAME, checkOutYear, checkOutMonth)
                .fetch(Records.mapping(RoomTypeMonthRevenue::new));
    }

    /**
     * The active room types; they get a column even without revenue (UC-020 A2).
     */
    public List<RevenueRoomType> findActiveRoomTypes() {
        return ctx.select(ROOM_TYPE.ID, ROOM_TYPE.NAME)
                .from(ROOM_TYPE)
                .where(ROOM_TYPE.ACTIVE.isTrue())
                .fetch(Records.mapping(RevenueRoomType::new));
    }
}
