package ch.martinelli.edu.hrs.reservation;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;

/**
 * Finds the rooms that are free for every night of a stay and large enough for the guests.
 */
@Service
@Transactional(readOnly = true)
public class AvailabilityService {

    public static final String ARRIVAL_IN_PAST = "Arrival date cannot be in the past";
    public static final String DEPARTURE_NOT_AFTER_ARRIVAL = "Departure date must be after arrival date";
    public static final String NO_GUESTS = "At least 1 guest is required";
    public static final String NO_ROOMS = "No rooms available for these dates";

    private final DSLContext ctx;
    private final Clock clock;

    public AvailabilityService(DSLContext ctx, Clock clock) {
        this.ctx = ctx;
        this.clock = clock;
    }

    public record AvailableRoom(long roomId, String roomNumber, long roomTypeId, String roomTypeName, int capacity,
                                BigDecimal nightlyRate, BigDecimal totalPrice) {
    }

    /**
     * @param excludedReservationId a reservation that does not block its room (the one being changed), or null
     */
    public List<AvailableRoom> findAvailableRooms(LocalDate arrival, LocalDate departure, Integer guests,
                                                  Long excludedReservationId) {
        validate(arrival, departure, guests);
        Condition blocking = ROOM.ID.eq(RESERVATION.ROOM_ID).and(overlaps(arrival, departure));
        if (excludedReservationId != null) {
            blocking = blocking.and(RESERVATION.ID.ne(excludedReservationId));
        }
        return ctx.select(ROOM.ID, ROOM.ROOM_NUMBER, ROOM_TYPE.ID, ROOM_TYPE.NAME, ROOM_TYPE.CAPACITY,
                        ROOM_TYPE.NIGHTLY_RATE)
                .from(ROOM).join(ROOM_TYPE).on(ROOM.ROOM_TYPE_ID.eq(ROOM_TYPE.ID))
                .where(ROOM_TYPE.CAPACITY.ge(guests))
                .andNotExists(DSL.selectOne().from(RESERVATION).where(blocking))
                .orderBy(ROOM.ROOM_NUMBER)
                .fetch(r -> new AvailableRoom(r.value1(), r.value2(), r.value3(), r.value4(), r.value5(), r.value6(),
                        totalPrice(r.value6(), arrival, departure)));
    }

    /**
     * Throws a {@link ReservationException} unless arrival is today or later, departure after arrival and at least
     * one guest.
     */
    public void validate(LocalDate arrival, LocalDate departure, Integer guests) {
        if (arrival.isBefore(today())) {
            throw new ReservationException(ARRIVAL_IN_PAST);
        }
        if (!departure.isAfter(arrival)) {
            throw new ReservationException(DEPARTURE_NOT_AFTER_ARRIVAL);
        }
        if (guests == null || guests < 1) {
            throw new ReservationException(NO_GUESTS);
        }
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /**
     * Reservations that are not cancelled and share at least one night with the stay; the departure day is not a
     * night of the stay.
     */
    static Condition overlaps(LocalDate arrival, LocalDate departure) {
        return RESERVATION.ARRIVAL_DATE.lt(departure)
                .and(RESERVATION.DEPARTURE_DATE.gt(arrival))
                .and(RESERVATION.STATUS.ne(ReservationService.CANCELLED));
    }

    public static BigDecimal totalPrice(BigDecimal nightlyRate, LocalDate arrival, LocalDate departure) {
        return nightlyRate.multiply(BigDecimal.valueOf(ChronoUnit.DAYS.between(arrival, departure)));
    }
}
