package ch.martinelli.edu.hrs.reception;

import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import org.jooq.DSLContext;
import org.jooq.Record3;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ch.martinelli.edu.hrs.db.Tables.GUEST;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_BLOCK;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;

/**
 * Reads the occupancy and cleaning status of every active room (UC-011 steps 2 and 3). Changes no data.
 */
@Repository
public class RoomStatusRepository {

    private final DSLContext ctx;

    public RoomStatusRepository(DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Every active room with its occupancy on this date, ordered by room number.
     */
    public List<RoomStatus> findRoomStatuses(LocalDate today) {
        // The room block covering today with the latest end date, per room
        Map<Long, Record3<Long, LocalDate, String>> blocks = new LinkedHashMap<>();
        ctx.select(ROOM_BLOCK.ROOM_ID, ROOM_BLOCK.END_DATE, ROOM_BLOCK.REASON)
                .from(ROOM_BLOCK)
                .where(ROOM_BLOCK.START_DATE.le(today))
                .and(ROOM_BLOCK.END_DATE.ge(today))
                .orderBy(ROOM_BLOCK.END_DATE)
                .forEach(block -> blocks.put(block.value1(), block));

        Map<Long, RoomStatus> rooms = new LinkedHashMap<>();
        ctx.select(ROOM.ID, ROOM.ROOM_NUMBER, ROOM.ROOM_TYPE_ID, ROOM_TYPE.NAME, ROOM.CLEANING_STATUS,
                        GUEST.FIRST_NAME, GUEST.LAST_NAME, RESERVATION.DEPARTURE_DATE)
                .from(ROOM)
                .join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(ROOM.ROOM_TYPE_ID))
                // UC-011 BR-001: Occupied when assigned to a reservation with status Checked In.
                .leftJoin(RESERVATION).on(RESERVATION.ROOM_ID.eq(ROOM.ID)
                        .and(RESERVATION.STATUS.eq(ReservationStatus.CHECKED_IN.name())))
                .leftJoin(GUEST).on(GUEST.ID.eq(RESERVATION.GUEST_ID))
                // UC-011 step 2: every active room
                .where(ROOM.ACTIVE.isTrue())
                .orderBy(ROOM.ROOM_NUMBER)
                .forEach(room -> {
                    Record3<Long, LocalDate, String> block = blocks.get(room.value1());
                    rooms.putIfAbsent(room.value1(), new RoomStatus(room.value1(), room.value2(), room.value3(),
                            room.value4(), "CLEAN".equals(room.value5()),
                            room.value6() == null ? null : room.value6() + " " + room.value7(), room.value8(),
                            block == null ? null : block.value2(), block == null ? null : block.value3()));
                });
        return List.copyOf(rooms.values());
    }
}
