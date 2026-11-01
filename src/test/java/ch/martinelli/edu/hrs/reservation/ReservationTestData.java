package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import org.jooq.DSLContext;

import java.math.BigDecimal;
import java.time.LocalDate;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;

/**
 * Inserts reservations directly, bypassing the service rules (e.g. for stays in the past).
 */
public final class ReservationTestData {

    public static final LocalDate MARCH_8 = LocalDate.of(2030, 3, 8);
    public static final LocalDate MARCH_10 = LocalDate.of(2030, 3, 10);
    public static final LocalDate MARCH_11 = LocalDate.of(2030, 3, 11);
    public static final LocalDate MARCH_12 = LocalDate.of(2030, 3, 12);
    public static final LocalDate MARCH_13 = LocalDate.of(2030, 3, 13);
    public static final LocalDate MARCH_14 = LocalDate.of(2030, 3, 14);

    private ReservationTestData() {
    }

    public static long roomId(DSLContext ctx, String roomNumber) {
        return ctx.select(ROOM.ID).from(ROOM).where(ROOM.ROOM_NUMBER.eq(roomNumber)).fetchSingle(ROOM.ID);
    }

    public static ReservationRecord insert(DSLContext ctx, String number, String roomNumber, LocalDate arrival,
                                           LocalDate departure, String guestName, String status) {
        ReservationRecord r = ctx.newRecord(RESERVATION);
        r.setReservationNumber(number);
        r.setRoomId(roomId(ctx, roomNumber));
        r.setArrivalDate(arrival);
        r.setDepartureDate(departure);
        r.setNumberOfGuests(1);
        r.setGuestName(guestName);
        r.setGuestEmail("guest@example.com");
        r.setNightlyRate(new BigDecimal("140.00"));
        r.setStatus(status);
        r.insert();
        return r;
    }
}
