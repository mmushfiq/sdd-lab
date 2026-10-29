package ch.martinelli.edu.hrs.reservation;

import org.jooq.DSLContext;

import java.math.BigDecimal;
import java.time.LocalDate;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;

/**
 * Direct database setup for tests, e.g. stays in the past that the service would reject. Days are relative to today.
 */
public record ReservationTestData(DSLContext dsl) {

    public static LocalDate day(int inDays) {
        return LocalDate.now().plusDays(inDays);
    }

    public static StayPeriod stay(int arrivalInDays, int departureInDays) {
        return new StayPeriod(day(arrivalInDays), day(departureInDays));
    }

    public long roomId(String number) {
        return dsl.select(ROOM.ID).from(ROOM).where(ROOM.NUMBER.eq(number)).fetchSingle(ROOM.ID);
    }

    public String insert(String bookingNumber, String roomNumber, int arrivalInDays, int departureInDays,
                         ReservationStatus status) {
        dsl.insertInto(RESERVATION)
                .set(RESERVATION.BOOKING_NUMBER, bookingNumber)
                .set(RESERVATION.ROOM_ID, roomId(roomNumber))
                .set(RESERVATION.ARRIVAL, day(arrivalInDays))
                .set(RESERVATION.DEPARTURE, day(departureInDays))
                .set(RESERVATION.NUMBER_OF_GUESTS, 1)
                .set(RESERVATION.GUEST_NAME, "Test Guest")
                .set(RESERVATION.GUEST_EMAIL, "test@example.com")
                .set(RESERVATION.GUEST_PHONE, "000")
                .set(RESERVATION.TOTAL_PRICE, new BigDecimal("100.00"))
                .set(RESERVATION.STATUS, status.name())
                .execute();
        return bookingNumber;
    }

    public void cancel(String bookingNumber) {
        dsl.update(RESERVATION).set(RESERVATION.STATUS, ReservationStatus.CANCELLED.name())
                .where(RESERVATION.BOOKING_NUMBER.eq(bookingNumber)).execute();
    }

    public void setOutOfService(String roomNumber, boolean outOfService) {
        dsl.update(ROOM).set(ROOM.OUT_OF_SERVICE, outOfService).where(ROOM.NUMBER.eq(roomNumber)).execute();
    }

    public void setRate(String roomTypeName, String rate) {
        dsl.update(ROOM_TYPE)
                .set(ROOM_TYPE.NIGHTLY_RATE, new BigDecimal(rate))
                .where(ROOM_TYPE.NAME.eq(roomTypeName)).execute();
    }

    public ReservationListItem find(ReservationService service, String bookingNumber) {
        return service.findReservations(new ReservationFilter(bookingNumber, null, null, null, false)).getFirst();
    }
}
