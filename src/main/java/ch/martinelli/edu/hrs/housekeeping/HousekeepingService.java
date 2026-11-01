package ch.martinelli.edu.hrs.housekeeping;

import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;

/**
 * The rooms housekeeping has to clean, and marking them as cleaned.
 */
@Service
@Transactional
public class HousekeepingService {

    public static final String CLEAN = "CLEAN";
    public static final String DIRTY = "DIRTY";

    private final DSLContext ctx;

    public HousekeepingService(DSLContext ctx) {
        this.ctx = ctx;
    }

    public record DirtyRoom(long roomId, String roomNumber, String roomTypeName) {
    }

    @Transactional(readOnly = true)
    public List<DirtyRoom> findDirtyRooms() {
        return ctx.select(ROOM.ID, ROOM.ROOM_NUMBER, ROOM_TYPE.NAME)
                .from(ROOM).join(ROOM_TYPE).on(ROOM.ROOM_TYPE_ID.eq(ROOM_TYPE.ID))
                .where(ROOM.CLEANING_STATUS.eq(DIRTY))
                .orderBy(ROOM.ROOM_NUMBER)
                .fetch(r -> new DirtyRoom(r.value1(), r.value2(), r.value3()));
    }

    /**
     * Unconditional, so marking a room another user has already cleaned is harmless.
     */
    public void markCleaned(long roomId) {
        ctx.update(ROOM).set(ROOM.CLEANING_STATUS, CLEAN).where(ROOM.ID.eq(roomId)).execute();
    }
}
