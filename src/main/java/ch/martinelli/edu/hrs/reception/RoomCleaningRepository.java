package ch.martinelli.edu.hrs.reception;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static ch.martinelli.edu.hrs.db.Tables.ROOM;

/**
 * Changes the cleaning status of rooms (UC-010 step 6, UC-014 steps 3 and 4).
 */
@Repository
public class RoomCleaningRepository {

    private final DSLContext ctx;

    public RoomCleaningRepository(DSLContext ctx) {
        this.ctx = ctx;
    }

    public void setDirty(Long roomId) {
        ctx.update(ROOM)
                .set(ROOM.CLEANING_STATUS, "DIRTY")
                .where(ROOM.ID.eq(roomId))
                .execute();
    }

    /**
     * Locks the room until the end of the current transaction and tells whether it is dirty (UC-014 step 3).
     */
    public boolean lockAndCheckDirty(Long roomId) {
        return "DIRTY".equals(ctx.select(ROOM.CLEANING_STATUS)
                .from(ROOM)
                .where(ROOM.ID.eq(roomId))
                .forUpdate()
                .fetchOptional(ROOM.CLEANING_STATUS)
                .orElse(null));
    }

    /**
     * Records the cleaning status Clean; occupancy and reservations stay unchanged (UC-014 step 4).
     */
    public void setClean(Long roomId) {
        ctx.update(ROOM)
                .set(ROOM.CLEANING_STATUS, "CLEAN")
                .where(ROOM.ID.eq(roomId))
                .execute();
    }
}
