package ch.martinelli.edu.hrs.reception;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static ch.martinelli.edu.hrs.db.Tables.ROOM;

/**
 * Changes the cleaning status of rooms (UC-010 step 6).
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
}
