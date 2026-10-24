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

/**
 * Reads and records room blocks (UC-018 steps 2, 5 and 6).
 */
@Repository
public class RoomBlockRepository {

    private final DSLContext ctx;

    public RoomBlockRepository(DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * The current and future room blocks, by first blocked date and room number.
     */
    public List<RoomBlockOverview> findCurrentAndFuture(LocalDate today) {
        return ctx.select(ROOM_BLOCK.ID, ROOM.ROOM_NUMBER, ROOM_BLOCK.START_DATE, ROOM_BLOCK.END_DATE,
                        ROOM_BLOCK.REASON)
                .from(ROOM_BLOCK)
                .join(ROOM).on(ROOM.ID.eq(ROOM_BLOCK.ROOM_ID))
                .where(ROOM_BLOCK.END_DATE.ge(today))
                .orderBy(ROOM_BLOCK.START_DATE, ROOM.ROOM_NUMBER)
                .fetch(Records.mapping(RoomBlockOverview::new));
    }

    /**
     * The active rooms, by room number.
     */
    public List<BlockableRoom> findActiveRooms() {
        return ctx.select(ROOM.ID, ROOM.ROOM_NUMBER, ROOM.ROOM_TYPE_ID, ROOM_TYPE.NAME)
                .from(ROOM)
                .join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(ROOM.ROOM_TYPE_ID))
                .where(ROOM.ACTIVE.isTrue())
                .orderBy(ROOM.ROOM_NUMBER)
                .fetch(Records.mapping(BlockableRoom::new));
    }

    /**
     * The active room, locked until the end of the transaction.
     */
    public Optional<BlockableRoom> findActiveRoomForUpdate(Long roomId) {
        return ctx.select(ROOM.ID, ROOM.ROOM_NUMBER, ROOM.ROOM_TYPE_ID, ROOM_TYPE.NAME)
                .from(ROOM)
                .join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(ROOM.ROOM_TYPE_ID))
                .where(ROOM.ID.eq(roomId))
                .and(ROOM.ACTIVE.isTrue())
                .forUpdate().of(ROOM)
                .fetchOptional(Records.mapping(BlockableRoom::new));
    }

    /**
     * Locks the room type until the end of the transaction; reservations lock the same row (UC-001 BR-005).
     */
    public void lockRoomType(Long roomTypeId) {
        ctx.select(ROOM_TYPE.ID).from(ROOM_TYPE).where(ROOM_TYPE.ID.eq(roomTypeId)).forUpdate().fetchOptional();
    }

    // UC-018 BR-001: Blocks of the same room do not overlap.
    public Optional<RoomBlockOverview> findOverlapping(Long roomId, LocalDate startDate, LocalDate endDate) {
        return ctx.select(ROOM_BLOCK.ID, ROOM.ROOM_NUMBER, ROOM_BLOCK.START_DATE, ROOM_BLOCK.END_DATE,
                        ROOM_BLOCK.REASON)
                .from(ROOM_BLOCK)
                .join(ROOM).on(ROOM.ID.eq(ROOM_BLOCK.ROOM_ID))
                .where(ROOM_BLOCK.ROOM_ID.eq(roomId))
                .and(ROOM_BLOCK.START_DATE.le(endDate))
                .and(ROOM_BLOCK.END_DATE.ge(startDate))
                .orderBy(ROOM_BLOCK.START_DATE)
                .limit(1)
                .fetchOptional(Records.mapping(RoomBlockOverview::new));
    }

    // UC-018 BR-002: A room cannot be blocked for a night on which it is assigned to a Checked In reservation.
    public Optional<String> findCheckedInReservationDuring(Long roomId, LocalDate startDate, LocalDate endDate) {
        return ctx.select(RESERVATION.RESERVATION_CODE)
                .from(RESERVATION)
                .where(RESERVATION.ROOM_ID.eq(roomId))
                .and(RESERVATION.STATUS.eq(ReservationStatus.CHECKED_IN.name()))
                .and(RESERVATION.ARRIVAL_DATE.le(endDate))
                .and(RESERVATION.DEPARTURE_DATE.gt(startDate))
                .orderBy(RESERVATION.ARRIVAL_DATE)
                .limit(1)
                .fetchOptional(RESERVATION.RESERVATION_CODE);
    }

    /**
     * The blocked nights on which the room type would have fewer active, unblocked rooms than Confirmed and Checked
     * In reservations once the room {@code roomId} is blocked.
     */
    // UC-018 BR-003: For every blocked night, the room type keeps at least as many active, unblocked rooms as
    // Confirmed and Checked In reservations.
    public List<LocalDate> findNightsWithoutRoom(Long roomTypeId, Long roomId, LocalDate startDate,
                                                 LocalDate endDate) {
        List<Long> roomIds = ctx.select(ROOM.ID)
                .from(ROOM)
                .where(ROOM.ROOM_TYPE_ID.eq(roomTypeId))
                .and(ROOM.ACTIVE.isTrue())
                .and(ROOM.ID.ne(roomId))
                .fetch(ROOM.ID);
        List<DateRange> blocks = ctx.select(ROOM_BLOCK.ROOM_ID, ROOM_BLOCK.START_DATE, ROOM_BLOCK.END_DATE)
                .from(ROOM_BLOCK)
                .where(ROOM_BLOCK.ROOM_ID.in(roomIds))
                .and(ROOM_BLOCK.START_DATE.le(endDate))
                .and(ROOM_BLOCK.END_DATE.ge(startDate))
                .fetch(Records.mapping(DateRange::new));
        List<DateRange> reservations = ctx.select(RESERVATION.ID, RESERVATION.ARRIVAL_DATE,
                        RESERVATION.DEPARTURE_DATE)
                .from(RESERVATION)
                .where(RESERVATION.ROOM_TYPE_ID.eq(roomTypeId))
                .and(RESERVATION.STATUS.in(ReservationStatus.CONFIRMED.name(), ReservationStatus.CHECKED_IN.name()))
                .and(RESERVATION.ARRIVAL_DATE.le(endDate))
                .and(RESERVATION.DEPARTURE_DATE.gt(startDate))
                .fetch(Records.mapping(DateRange::new));

        List<LocalDate> nights = new ArrayList<>();
        for (LocalDate night = startDate; !night.isAfter(endDate); night = night.plusDays(1)) {
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

    public Long insert(RoomBlockData data) {
        return ctx.insertInto(ROOM_BLOCK)
                .set(ROOM_BLOCK.ROOM_ID, data.roomId())
                .set(ROOM_BLOCK.START_DATE, data.startDate())
                .set(ROOM_BLOCK.END_DATE, data.endDate())
                .set(ROOM_BLOCK.REASON, data.reason())
                .returning(ROOM_BLOCK.ID)
                .fetchSingle(ROOM_BLOCK.ID);
    }

    private record DateRange(Long key, LocalDate from, LocalDate to) {
    }
}
