package ch.martinelli.edu.hrs.management;

import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_BLOCK;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static org.jooq.impl.DSL.max;

/**
 * Reads and records rooms (UC-017 steps 2, 6 and 7).
 */
@Repository
public class RoomRepository {

    private final DSLContext ctx;

    public RoomRepository(DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * All rooms, also the inactive ones, ordered by room number.
     */
    public List<RoomOverview> findAll() {
        return ctx.select(ROOM.ID, ROOM.ROOM_NUMBER, ROOM.ROOM_TYPE_ID, ROOM_TYPE.NAME,
                        ROOM.CLEANING_STATUS.eq("CLEAN"), ROOM.ACTIVE)
                .from(ROOM)
                .join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(ROOM.ROOM_TYPE_ID))
                .orderBy(ROOM.ROOM_NUMBER)
                .fetch(Records.mapping(RoomOverview::new));
    }

    /**
     * The room, locked until the end of the transaction.
     */
    public Optional<RoomOverview> findForUpdate(Long roomId) {
        return ctx.select(ROOM.ID, ROOM.ROOM_NUMBER, ROOM.ROOM_TYPE_ID, ROOM_TYPE.NAME,
                        ROOM.CLEANING_STATUS.eq("CLEAN"), ROOM.ACTIVE)
                .from(ROOM)
                .join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(ROOM.ROOM_TYPE_ID))
                .where(ROOM.ID.eq(roomId))
                .forUpdate().of(ROOM)
                .fetchOptional(Records.mapping(RoomOverview::new));
    }

    public boolean existsOtherWithNumber(String roomNumber, Long excludedId) {
        return ctx.fetchExists(ROOM, ROOM.ROOM_NUMBER.eq(roomNumber)
                .and(excludedId == null ? ROOM.ID.isNotNull() : ROOM.ID.ne(excludedId)));
    }

    public boolean roomTypeExists(Long roomTypeId) {
        return ctx.fetchExists(ROOM_TYPE, ROOM_TYPE.ID.eq(roomTypeId));
    }

    /**
     * Locks the room type until the end of the transaction; reservations lock the same row (UC-001 BR-005).
     */
    public void lockRoomType(Long roomTypeId) {
        ctx.select(ROOM_TYPE.ID).from(ROOM_TYPE).where(ROOM_TYPE.ID.eq(roomTypeId)).forUpdate().fetchOptional();
    }

    public boolean isOccupied(Long roomId) {
        return ctx.fetchExists(RESERVATION, RESERVATION.ROOM_ID.eq(roomId)
                .and(RESERVATION.STATUS.eq(ReservationStatus.CHECKED_IN.name())));
    }

    /**
     * The nights from today on on which the room type would have fewer active, unblocked rooms than Confirmed and
     * Checked In reservations when the room {@code removedRoomId} no longer counted for it.
     */
    // UC-017 BR-003: For every future night, every affected room type keeps at least as many active, unblocked rooms
    // as Confirmed and Checked In reservations.
    public List<LocalDate> findNightsWithoutRoom(Long roomTypeId, Long removedRoomId, LocalDate today) {
        List<ReservationStatus> taking = List.of(ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN);
        LocalDate lastDeparture = ctx.select(max(RESERVATION.DEPARTURE_DATE))
                .from(RESERVATION)
                .where(RESERVATION.ROOM_TYPE_ID.eq(roomTypeId))
                .and(RESERVATION.STATUS.in(taking.stream().map(Enum::name).toList()))
                .fetchOne(0, LocalDate.class);
        if (lastDeparture == null || !lastDeparture.isAfter(today)) {
            return List.of();
        }
        List<Long> roomIds = ctx.select(ROOM.ID)
                .from(ROOM)
                .where(ROOM.ROOM_TYPE_ID.eq(roomTypeId))
                .and(ROOM.ACTIVE.isTrue())
                .and(ROOM.ID.ne(removedRoomId))
                .fetch(ROOM.ID);
        List<DateRange> blocks = ctx.select(ROOM_BLOCK.ROOM_ID, ROOM_BLOCK.START_DATE, ROOM_BLOCK.END_DATE)
                .from(ROOM_BLOCK)
                .where(ROOM_BLOCK.ROOM_ID.in(roomIds))
                .and(ROOM_BLOCK.END_DATE.ge(today))
                .fetch(Records.mapping(DateRange::new));
        List<DateRange> reservations = ctx.select(RESERVATION.ID, RESERVATION.ARRIVAL_DATE,
                        RESERVATION.DEPARTURE_DATE)
                .from(RESERVATION)
                .where(RESERVATION.ROOM_TYPE_ID.eq(roomTypeId))
                .and(RESERVATION.STATUS.in(taking.stream().map(Enum::name).toList()))
                .and(RESERVATION.DEPARTURE_DATE.gt(today))
                .fetch(Records.mapping(DateRange::new));

        List<LocalDate> nights = new ArrayList<>();
        for (LocalDate night = today; night.isBefore(lastDeparture); night = night.plusDays(1)) {
            LocalDate n = night;
            long blockedRooms = blocks.stream()
                    .filter(block -> !n.isBefore(block.from()) && !n.isAfter(block.to()))
                    .map(DateRange::key)
                    .distinct()
                    .count();
            long reserved = reservations.stream()
                    .filter(reservation -> !n.isBefore(reservation.from()) && n.isBefore(reservation.to()))
                    .count();
            if (roomIds.size() - blockedRooms < reserved) {
                nights.add(night);
            }
        }
        return nights;
    }

    // UC-017 BR-002: A new room starts with cleaning status Clean.
    public Long insert(RoomData data) {
        return ctx.insertInto(ROOM)
                .set(ROOM.ROOM_NUMBER, data.roomNumber())
                .set(ROOM.ROOM_TYPE_ID, data.roomTypeId())
                .set(ROOM.CLEANING_STATUS, "CLEAN")
                .set(ROOM.ACTIVE, data.active())
                .returning(ROOM.ID)
                .fetchSingle(ROOM.ID);
    }

    public void update(Long roomId, RoomData data) {
        ctx.update(ROOM)
                .set(ROOM.ROOM_NUMBER, data.roomNumber())
                .set(ROOM.ROOM_TYPE_ID, data.roomTypeId())
                .set(ROOM.ACTIVE, data.active())
                .where(ROOM.ID.eq(roomId))
                .execute();
    }

    private record DateRange(Long key, LocalDate from, LocalDate to) {
    }
}
