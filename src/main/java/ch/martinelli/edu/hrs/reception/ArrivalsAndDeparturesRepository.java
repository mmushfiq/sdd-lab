package ch.martinelli.edu.hrs.reception;

import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.GUEST;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;

/**
 * Reads today's arrivals and departures (UC-008 steps 2 and 3). Changes no data.
 */
@Repository
public class ArrivalsAndDeparturesRepository {

    private final DSLContext ctx;

    public ArrivalsAndDeparturesRepository(DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * The reservations arriving on this date, ordered by guest last name.
     */
    public List<Arrival> findArrivals(LocalDate today) {
        return ctx.select(RESERVATION.RESERVATION_CODE, GUEST.FIRST_NAME, GUEST.LAST_NAME, ROOM_TYPE.NAME,
                        RESERVATION.ARRIVAL_DATE, RESERVATION.DEPARTURE_DATE, RESERVATION.NUMBER_OF_GUESTS,
                        RESERVATION.STATUS.convertFrom(ReservationStatus::valueOf))
                .from(RESERVATION)
                .join(GUEST).on(GUEST.ID.eq(RESERVATION.GUEST_ID))
                .join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(RESERVATION.ROOM_TYPE_ID))
                // UC-008 BR-001: Arrival date today and status Confirmed or Checked In.
                .where(RESERVATION.ARRIVAL_DATE.eq(today))
                .and(RESERVATION.STATUS.in(ReservationStatus.CONFIRMED.name(), ReservationStatus.CHECKED_IN.name()))
                .orderBy(GUEST.LAST_NAME, GUEST.FIRST_NAME, RESERVATION.RESERVATION_CODE)
                .fetch(Records.mapping(Arrival::new));
    }

    /**
     * The reservations departing on this date and the overdue ones, ordered by room number.
     */
    public List<Departure> findDepartures(LocalDate today) {
        return ctx.select(RESERVATION.RESERVATION_CODE, GUEST.FIRST_NAME, GUEST.LAST_NAME, ROOM.ROOM_NUMBER,
                        RESERVATION.TOTAL_PRICE, RESERVATION.DEPARTURE_DATE,
                        RESERVATION.STATUS.convertFrom(ReservationStatus::valueOf),
                        // UC-008 A2: Checked In although the departure date is before today.
                        RESERVATION.DEPARTURE_DATE.lt(today))
                .from(RESERVATION)
                .join(GUEST).on(GUEST.ID.eq(RESERVATION.GUEST_ID))
                .leftJoin(ROOM).on(ROOM.ID.eq(RESERVATION.ROOM_ID))
                // UC-008 BR-002: Departure date today and status Checked In or Checked Out, plus every overdue
                // reservation (status Checked In, departure date before today).
                .where(RESERVATION.DEPARTURE_DATE.eq(today)
                        .and(RESERVATION.STATUS.in(ReservationStatus.CHECKED_IN.name(),
                                ReservationStatus.CHECKED_OUT.name())))
                .or(RESERVATION.DEPARTURE_DATE.lt(today)
                        .and(RESERVATION.STATUS.eq(ReservationStatus.CHECKED_IN.name())))
                .orderBy(ROOM.ROOM_NUMBER, RESERVATION.RESERVATION_CODE)
                .fetch(Records.mapping(Departure::new));
    }
}
