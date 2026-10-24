package ch.martinelli.edu.hrs.housekeeping;

import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_BLOCK;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static org.jooq.impl.DSL.exists;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.not;
import static org.jooq.impl.DSL.selectOne;

/**
 * Reads the dirty rooms for the cleaning list (UC-013 step 2).
 */
@Repository
public class CleaningListRepository {

    private final DSLContext ctx;

    public CleaningListRepository(DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Every active, dirty room that is not blocked on this date, ordered by room number.
     */
    public List<CleaningListEntry> findDirtyRooms(LocalDate today) {
        return ctx.select(ROOM.ID, ROOM.ROOM_NUMBER, ROOM_TYPE.NAME,
                        field(exists(selectOne().from(RESERVATION)
                                .where(RESERVATION.ROOM_ID.eq(ROOM.ID))
                                .and(RESERVATION.STATUS.eq(ReservationStatus.CHECKED_IN.name())))),
                        field(exists(selectOne().from(RESERVATION)
                                .where(RESERVATION.ROOM_ID.eq(ROOM.ID))
                                .and(RESERVATION.STATUS.eq(ReservationStatus.CHECKED_OUT.name()))
                                .and(RESERVATION.CHECKED_OUT_AT.ge(today.atStartOfDay()))
                                .and(RESERVATION.CHECKED_OUT_AT.lt(today.plusDays(1).atStartOfDay())))),
                        field(exists(selectOne().from(RESERVATION)
                                .where(RESERVATION.ROOM_ID.eq(ROOM.ID))
                                .and(RESERVATION.STATUS.eq(ReservationStatus.CHECKED_IN.name()))
                                .and(RESERVATION.DEPARTURE_DATE.eq(today)))))
                .from(ROOM)
                .join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(ROOM.ROOM_TYPE_ID))
                // UC-013 step 2: every active, dirty room
                .where(ROOM.ACTIVE.isTrue())
                .and(ROOM.CLEANING_STATUS.eq("DIRTY"))
                // UC-013 BR-002: Rooms blocked today are not on the cleaning list.
                .and(not(exists(selectOne().from(ROOM_BLOCK)
                        .where(ROOM_BLOCK.ROOM_ID.eq(ROOM.ID))
                        .and(ROOM_BLOCK.START_DATE.le(today))
                        .and(ROOM_BLOCK.END_DATE.ge(today)))))
                .orderBy(ROOM.ROOM_NUMBER)
                .fetch(Records.mapping(CleaningListEntry::new));
    }

    /**
     * The room with this id when it is on the cleaning list on this date.
     */
    public Optional<CleaningListEntry> findDirtyRoom(Long roomId, LocalDate today) {
        return findDirtyRooms(today).stream().filter(room -> room.roomId().equals(roomId)).findFirst();
    }
}
