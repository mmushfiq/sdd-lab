package ch.martinelli.edu.hrs.report;

import ch.martinelli.edu.hrs.report.ReportService.MonthFigures;
import ch.martinelli.edu.hrs.report.ReportService.YearReport;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static ch.martinelli.edu.hrs.reservation.ReservationTestData.insert;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ReportServiceTest {

    private static final int YEAR = 2031;

    @Autowired
    DSLContext ctx;

    @Autowired
    ReportService reportService;

    // Scenario "Booked and cancelled reservations not counted"
    @Test
    void booked_and_cancelled_not_counted() {
        insert(ctx, "BOOKED01", "104", day(5, 1), day(5, 5), "Booked", "BOOKED");
        insert(ctx, "CANCEL01", "105", day(5, 1), day(5, 5), "Cancelled", "CANCELLED");

        MonthFigures may = month(reportService.yearReport(YEAR), Month.MAY);

        assertThat(may.roomNights()).isZero();
        assertThat(may.revenue()).isEqualByComparingTo("0.00");
    }

    // Scenario "Stay across two months"
    @Test
    void stay_across_two_months_split_by_night() {
        insert(ctx, "SPLIT001", "104", day(3, 30), day(4, 2), "Split", "CHECKED_OUT");

        YearReport report = reportService.yearReport(YEAR);

        assertThat(month(report, Month.MARCH).roomNights()).isEqualTo(2);
        assertThat(month(report, Month.APRIL).roomNights()).isEqualTo(1);
    }

    // Scenario "Occupancy computed"; Checked in stays count too
    @Test
    void sixty_nights_in_april_with_ten_rooms_is_twenty_percent() {
        String[] rooms = {"101", "102", "103", "104", "105", "106"};
        for (int i = 0; i < rooms.length; i++) {
            insert(ctx, "OCC0000" + i, rooms[i], day(4, 1), day(4, 11), "Guest " + i,
                    i == 0 ? "CHECKED_IN" : "CHECKED_OUT");
        }

        MonthFigures april = month(reportService.yearReport(YEAR), Month.APRIL);

        assertThat(april.roomNights()).isEqualTo(60);
        assertThat(april.occupancyPercent()).isEqualByComparingTo("20.0");
        assertThat(april.occupancyPercent().scale()).isEqualTo(1);
    }

    // Scenarios "Revenue from saved rates" and "Rate changed after the stay"
    @Test
    void revenue_from_saved_rates_survives_rate_change() {
        insert(ctx, "REV00001", "104", day(4, 1), day(4, 4), "Anna", "CHECKED_OUT");
        insertWithRate("REV00002", "109", day(4, 10), day(4, 12), "220.00");

        assertThat(month(reportService.yearReport(YEAR), Month.APRIL).revenue()).isEqualByComparingTo("860.00");

        ctx.update(ROOM_TYPE).set(ROOM_TYPE.NIGHTLY_RATE, new BigDecimal("999.00")).execute();

        MonthFigures april = month(reportService.yearReport(YEAR), Month.APRIL);
        assertThat(april.revenue()).isEqualByComparingTo("860.00");
        assertThat(april.revenue().scale()).isEqualTo(2);
    }

    // Scenario "Year without stays"
    @Test
    void year_without_stays_shows_zeros() {
        YearReport report = reportService.yearReport(2025);

        assertThat(report.months()).hasSize(12).allSatisfy(m -> {
            assertThat(m.roomNights()).isZero();
            assertThat(m.occupancyPercent()).isEqualByComparingTo("0.0");
            assertThat(m.revenue()).isEqualByComparingTo("0.00");
        });
        assertThat(report.total().roomNights()).isZero();
    }

    // Year report: twelve months and a total that sums them; nights outside the year are skipped
    @Test
    void total_sums_the_months() {
        insert(ctx, "YEAR0001", "104", day(1, 10), day(1, 15), "Jan", "CHECKED_OUT");
        insert(ctx, "YEAR0002", "105", day(12, 30), LocalDate.of(YEAR + 1, 1, 3), "Dec", "CHECKED_OUT");

        YearReport report = reportService.yearReport(YEAR);

        assertThat(report.year()).isEqualTo(YEAR);
        assertThat(report.months()).extracting(m -> m.month().getMonth()).containsExactly(Month.values());
        assertThat(report.total().roomNights()).isEqualTo(7);
        assertThat(report.total().revenue()).isEqualByComparingTo("980.00");
        // 7 / (10 rooms x 365 days) = 0.19 %
        assertThat(report.total().occupancyPercent()).isEqualByComparingTo("0.2");
    }

    private void insertWithRate(String number, String room, LocalDate arrival, LocalDate departure, String rate) {
        insert(ctx, number, room, arrival, departure, "Guest", "CHECKED_OUT");
        ctx.update(RESERVATION).set(RESERVATION.NIGHTLY_RATE, new BigDecimal(rate))
                .where(RESERVATION.RESERVATION_NUMBER.eq(number)).execute();
    }

    private static LocalDate day(int month, int day) {
        return LocalDate.of(YEAR, month, day);
    }

    private static MonthFigures month(YearReport report, Month month) {
        return report.months().get(month.ordinal());
    }
}
