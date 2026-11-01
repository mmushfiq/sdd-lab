package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import org.jooq.DSLContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION_NIGHT;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static ch.martinelli.edu.hrs.db.Tables.SEASONAL_RATE;

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
    public static final LocalDate DECEMBER_18 = LocalDate.of(2030, 12, 18);
    public static final LocalDate DECEMBER_20 = LocalDate.of(2030, 12, 20);
    public static final LocalDate DECEMBER_21 = LocalDate.of(2030, 12, 21);
    public static final LocalDate JANUARY_6 = LocalDate.of(2031, 1, 6);

    private ReservationTestData() {
    }

    public static long roomId(DSLContext ctx, String roomNumber) {
        return ctx.select(ROOM.ID).from(ROOM).where(ROOM.ROOM_NUMBER.eq(roomNumber)).fetchSingle(ROOM.ID);
    }

    /**
     * Inserts a reservation with the rate 140.00 for each night.
     */
    public static long roomTypeId(DSLContext ctx, String roomTypeName) {
        return ctx.select(ROOM_TYPE.ID).from(ROOM_TYPE).where(ROOM_TYPE.NAME.eq(roomTypeName)).fetchSingle(ROOM_TYPE.ID);
    }

    /**
     * Inserts a seasonal rate covering the nights from first to last night, both included.
     */
    public static long insertSeasonalRate(DSLContext ctx, String roomTypeName, String name, LocalDate firstNight,
                                          LocalDate lastNight, String rate) {
        return ctx.insertInto(SEASONAL_RATE)
                .set(SEASONAL_RATE.ROOM_TYPE_ID, roomTypeId(ctx, roomTypeName))
                .set(SEASONAL_RATE.NAME, name)
                .set(SEASONAL_RATE.FIRST_NIGHT, firstNight)
                .set(SEASONAL_RATE.LAST_NIGHT, lastNight)
                .set(SEASONAL_RATE.NIGHTLY_RATE, new BigDecimal(rate))
                .returningResult(SEASONAL_RATE.ID).fetchSingle(SEASONAL_RATE.ID);
    }

    public static ReservationRecord insert(DSLContext ctx, String number, String roomNumber, LocalDate arrival,
                                           LocalDate departure, String guestName, String status) {
        String[] rates = new String[(int) ChronoUnit.DAYS.between(arrival, departure)];
        Arrays.fill(rates, "140.00");
        return insert(ctx, number, roomNumber, arrival, departure, guestName, status, rates);
    }

    /**
     * Inserts a reservation with the given rate for each night, in date order.
     */
    public static ReservationRecord insert(DSLContext ctx, String number, String roomNumber, LocalDate arrival,
                                           LocalDate departure, String guestName, String status,
                                           String... nightRates) {
        ReservationRecord r = ctx.newRecord(RESERVATION);
        r.setReservationNumber(number);
        r.setRoomId(roomId(ctx, roomNumber));
        r.setArrivalDate(arrival);
        r.setDepartureDate(departure);
        r.setNumberOfGuests(1);
        r.setGuestName(guestName);
        r.setGuestEmail("guest@example.com");
        r.setStatus(status);
        r.insert();
        setNightRates(ctx, r, nightRates);
        return r;
    }

    /**
     * Replaces the reservation's night rows with the given rates, one per night from the arrival date.
     */
    public static void setNightRates(DSLContext ctx, ReservationRecord r, String... nightRates) {
        if (nightRates.length != ChronoUnit.DAYS.between(r.getArrivalDate(), r.getDepartureDate())) {
            throw new IllegalArgumentException("One rate per night expected");
        }
        ctx.deleteFrom(RESERVATION_NIGHT).where(RESERVATION_NIGHT.RESERVATION_ID.eq(r.getId())).execute();
        for (int i = 0; i < nightRates.length; i++) {
            ctx.insertInto(RESERVATION_NIGHT)
                    .set(RESERVATION_NIGHT.RESERVATION_ID, r.getId())
                    .set(RESERVATION_NIGHT.NIGHT_DATE, r.getArrivalDate().plusDays(i))
                    .set(RESERVATION_NIGHT.NIGHTLY_RATE, new BigDecimal(nightRates[i]))
                    .execute();
        }
    }
}
