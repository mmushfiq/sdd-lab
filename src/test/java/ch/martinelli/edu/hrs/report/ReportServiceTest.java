package ch.martinelli.edu.hrs.report;

import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.reservation.BookingRequest;
import ch.martinelli.edu.hrs.reservation.BookingSource;
import ch.martinelli.edu.hrs.reservation.ReservationService;
import ch.martinelli.edu.hrs.room.RoomService;
import ch.martinelli.edu.hrs.room.RoomTypeService;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

import static ch.martinelli.edu.hrs.db.Sequences.RESERVATION_SEQ;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION_NIGHT;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ReportServiceTest {

    static final YearMonth MARCH = YearMonth.of(2031, 3);

    @Autowired
    private ReportService reportService;

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private Clock clock;

    private long roomA;
    private long roomB;

    @BeforeEach
    void createRooms() {
        var type = roomTypeService.save(roomType("Test Report", 2, "100.00"));
        roomA = room("T851", type.getId());
        roomB = room("T852", type.getId());
    }

    @Test
    void occupancyRevenueAndOnlineShareOfAMonth() {
        stay(dsl, roomA, MARCH.atDay(10), MARCH.atDay(13), "CHECKED_OUT", "100.00", "RECEPTION",
                MARCH.minusMonths(1).atDay(1));
        stay(dsl, roomB, MARCH.atDay(30), MARCH.plusMonths(1).atDay(2), "CHECKED_IN", "150.00", "ONLINE",
                MARCH.atDay(5));
        stay(dsl, roomA, MARCH.atDay(20), MARCH.atDay(22), "BOOKED", "100.00", "RECEPTION", MARCH.atDay(6));
        stay(dsl, roomB, MARCH.atDay(25), MARCH.atDay(27), "CANCELLED", "100.00", "ONLINE", MARCH.atDay(7));

        var report = reportService.monthlyReport(MARCH);

        var rooms = dsl.fetchCount(ROOM);
        assertThat(report.occupiedNights()).isEqualTo(5);
        assertThat(report.availableNights()).isEqualTo(rooms * 31);
        assertThat(report.occupancyPercent()).isEqualByComparingTo(
                BigDecimal.valueOf(500).divide(BigDecimal.valueOf(rooms * 31L), 1, java.math.RoundingMode.HALF_UP));
        assertThat(report.revenue()).isEqualByComparingTo("600.00");
        assertThat(report.createdReservations()).isEqualTo(3);
        assertThat(report.onlineReservations()).isEqualTo(2);
        assertThat(report.onlineSharePercent()).isEqualByComparingTo("66.7");
    }

    @Test
    void stayAcrossMonthEndCountsOnlyTheMonthsOwnNights() {
        stay(dsl, roomB, MARCH.atDay(30), MARCH.plusMonths(1).atDay(2), "CHECKED_OUT", "150.00", "ONLINE",
                MARCH.atDay(5));

        assertThat(reportService.monthlyReport(MARCH).occupiedNights()).isEqualTo(2);
        var april = reportService.monthlyReport(MARCH.plusMonths(1));
        assertThat(april.occupiedNights()).isEqualTo(1);
        assertThat(april.revenue()).isEqualByComparingTo("150.00");
    }

    @Test
    void monthWithoutReservationsHasNoOnlineShare() {
        var report = reportService.monthlyReport(YearMonth.of(2031, 7));

        assertThat(report.occupiedNights()).isZero();
        assertThat(report.revenue()).isEqualByComparingTo("0");
        assertThat(report.onlineSharePercent()).isNull();
    }

    @Test
    void stayBookedThroughTheServiceCountsAfterCheckOut() {
        var today = LocalDate.now(clock);
        var month = YearMonth.from(today);
        var before = reportService.monthlyReport(month);
        var reservation = reservationService.book(new BookingRequest(roomA, today, today.plusDays(2), 1,
                "Test Report Guest", "report@example.com", "+41 79 000 00 00", BookingSource.RECEPTION));
        reservationService.checkIn(reservation.getId());
        reservationService.checkOut(reservation.getId());

        var after = reportService.monthlyReport(month);

        var nightsInMonth = today.plusDays(1).getMonth() == today.getMonth() ? 2 : 1;
        assertThat(after.occupiedNights() - before.occupiedNights()).isEqualTo(nightsInMonth);
        assertThat(after.revenue().subtract(before.revenue())).isEqualByComparingTo(
                BigDecimal.valueOf(100L * nightsInMonth));
    }

    private long room(String number, long typeId) {
        var room = new RoomRecord();
        room.setRoomNumber(number);
        room.setRoomTypeId(typeId);
        return roomService.save(room).getId();
    }

    /**
     * Booking cannot create past or checked-in stays, so report data is inserted directly, with one night row per
     * night at the given rate (003 FR-009).
     */
    static void stay(DSLContext dsl, long roomId, LocalDate arrival, LocalDate departure, String status,
                     String rate, String source, LocalDate createdOn) {
        var id = dsl.nextval(RESERVATION_SEQ);
        var nightlyRate = new BigDecimal(rate);
        dsl.insertInto(RESERVATION)
                .set(RESERVATION.ID, id)
                .set(RESERVATION.RESERVATION_NUMBER, "RP%06d".formatted(id % 1_000_000))
                .set(RESERVATION.ROOM_ID, roomId)
                .set(RESERVATION.ARRIVAL_DATE, arrival)
                .set(RESERVATION.DEPARTURE_DATE, departure)
                .set(RESERVATION.NUMBER_OF_GUESTS, 1)
                .set(RESERVATION.GUEST_NAME, "Test Report Guest")
                .set(RESERVATION.TOTAL_PRICE, nightlyRate.multiply(
                        BigDecimal.valueOf(departure.toEpochDay() - arrival.toEpochDay())))
                .set(RESERVATION.STATUS, status)
                .set(RESERVATION.SOURCE, source)
                .set(RESERVATION.CREATED_AT, createdOn.atTime(12, 0))
                .execute();
        for (var night = arrival; night.isBefore(departure); night = night.plusDays(1)) {
            dsl.insertInto(RESERVATION_NIGHT)
                    .set(RESERVATION_NIGHT.RESERVATION_ID, id)
                    .set(RESERVATION_NIGHT.NIGHT_DATE, night)
                    .set(RESERVATION_NIGHT.NIGHTLY_RATE, nightlyRate)
                    .execute();
        }
    }
}
