package ch.martinelli.edu.hrs.frontdesk;

import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.housekeeping.HousekeepingService;
import ch.martinelli.edu.hrs.reservation.ReservationException;
import ch.martinelli.edu.hrs.reservation.ReservationService;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION_NIGHT;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;

/**
 * Room overview for reception, check-in and check-out. Check-out makes the room dirty in the same transaction, so
 * the cleaning list never misses a departure.
 */
@Service
@Transactional
public class FrontDeskService {

    public static final String CHECKED_IN = "CHECKED_IN";
    public static final String CHECKED_OUT = "CHECKED_OUT";

    public static final String NOT_CLEAN = "The room is not clean yet";
    public static final String STILL_OCCUPIED = "The room is still occupied";
    public static final String NOT_ARRIVAL_DATE = "Check-in is only possible on the arrival date";
    public static final String NOT_BOOKED = "Only Booked reservations can be checked in";
    public static final String NOT_CHECKED_IN = "Only Checked in reservations can be checked out";

    private final DSLContext ctx;
    private final Clock clock;

    public FrontDeskService(DSLContext ctx, Clock clock) {
        this.ctx = ctx;
        this.clock = clock;
    }

    public enum RoomState {
        FREE("Free"), ARRIVAL_TODAY("Arrival today"), OCCUPIED("Occupied");

        private final String label;

        RoomState(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /**
     * reservationId, guestName and departureDate are null for a Free room.
     */
    public record RoomOverview(long roomId, String roomNumber, String roomTypeName, String cleaningStatus,
                               RoomState state, Long reservationId, String guestName, LocalDate departureDate) {
    }

    @Transactional(readOnly = true)
    public List<RoomOverview> roomOverview() {
        LocalDate today = LocalDate.now(clock);
        // A Checked in reservation wins over a Booked one arriving today (previous guest not checked out yet)
        Map<Long, ReservationRecord> current = ctx.selectFrom(RESERVATION)
                .where(RESERVATION.STATUS.eq(CHECKED_IN))
                .or(RESERVATION.STATUS.eq(ReservationService.BOOKED).and(RESERVATION.ARRIVAL_DATE.eq(today)))
                .fetch().stream()
                .collect(Collectors.toMap(ReservationRecord::getRoomId, Function.identity(),
                        (a, b) -> CHECKED_IN.equals(a.getStatus()) ? a : b));
        return ctx.select(ROOM.ID, ROOM.ROOM_NUMBER, ROOM_TYPE.NAME, ROOM.CLEANING_STATUS)
                .from(ROOM).join(ROOM_TYPE).on(ROOM.ROOM_TYPE_ID.eq(ROOM_TYPE.ID))
                .orderBy(ROOM.ROOM_NUMBER)
                .fetch(r -> {
                    ReservationRecord res = current.get(r.value1());
                    if (res == null) {
                        return new RoomOverview(r.value1(), r.value2(), r.value3(), r.value4(), RoomState.FREE,
                                null, null, null);
                    }
                    RoomState state = CHECKED_IN.equals(res.getStatus()) ? RoomState.OCCUPIED : RoomState.ARRIVAL_TODAY;
                    return new RoomOverview(r.value1(), r.value2(), r.value3(), r.value4(), state, res.getId(),
                            res.getGuestName(), res.getDepartureDate());
                });
    }

    /**
     * Checks in a Booked reservation on its arrival date, if its room is clean and nobody is checked in there.
     */
    public void checkIn(long reservationId) {
        ReservationRecord reservation = ctx.fetchSingle(RESERVATION, RESERVATION.ID.eq(reservationId));
        if (!ReservationService.BOOKED.equals(reservation.getStatus())) {
            throw new ReservationException(NOT_BOOKED);
        }
        if (!reservation.getArrivalDate().equals(LocalDate.now(clock))) {
            throw new ReservationException(NOT_ARRIVAL_DATE);
        }
        String cleaningStatus = ctx.select(ROOM.CLEANING_STATUS).from(ROOM)
                .where(ROOM.ID.eq(reservation.getRoomId())).forUpdate().fetchSingle(ROOM.CLEANING_STATUS);
        if (!HousekeepingService.CLEAN.equals(cleaningStatus)) {
            throw new ReservationException(NOT_CLEAN);
        }
        if (ctx.fetchExists(RESERVATION, RESERVATION.ROOM_ID.eq(reservation.getRoomId())
                .and(RESERVATION.STATUS.eq(CHECKED_IN)))) {
            throw new ReservationException(STILL_OCCUPIED);
        }
        reservation.setStatus(CHECKED_IN);
        reservation.update();
    }

    /**
     * Checks out a Checked in reservation, shortens an early stay to the nights actually stayed (at least one), drops
     * the rates of the nights not stayed and makes the room dirty.
     */
    public void checkOut(long reservationId) {
        ReservationRecord reservation = ctx.fetchSingle(RESERVATION, RESERVATION.ID.eq(reservationId));
        if (!CHECKED_IN.equals(reservation.getStatus())) {
            throw new ReservationException(NOT_CHECKED_IN);
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate departure = today.isBefore(reservation.getDepartureDate()) ? today : reservation.getDepartureDate();
        LocalDate minDeparture = reservation.getArrivalDate().plusDays(1);
        reservation.setDepartureDate(departure.isBefore(minDeparture) ? minDeparture : departure);
        reservation.setStatus(CHECKED_OUT);
        reservation.update();
        ctx.deleteFrom(RESERVATION_NIGHT).where(RESERVATION_NIGHT.RESERVATION_ID.eq(reservationId))
                .and(RESERVATION_NIGHT.NIGHT_DATE.ge(reservation.getDepartureDate())).execute();
        ctx.update(ROOM).set(ROOM.CLEANING_STATUS, HousekeepingService.DIRTY)
                .where(ROOM.ID.eq(reservation.getRoomId())).execute();
    }
}
