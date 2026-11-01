package ch.martinelli.edu.hrs.room;

import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import org.jooq.DSLContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;

/**
 * Maintains room types and rooms. Records without an id are created, records with an id are updated.
 * Uniqueness of names and numbers is left to the database's unique constraints.
 */
@Service
@Transactional
public class RoomService {

    public static final String ROOM_TYPE_NAME_USED = "A room type with this name already exists";
    public static final String ROOM_NUMBER_USED = "A room with this number already exists";

    private final DSLContext ctx;

    public RoomService(DSLContext ctx) {
        this.ctx = ctx;
    }

    @Transactional(readOnly = true)
    public List<RoomTypeRecord> findAllRoomTypes() {
        return ctx.selectFrom(ROOM_TYPE).orderBy(ROOM_TYPE.NAME).fetch();
    }

    @Transactional(readOnly = true)
    public List<RoomRecord> findAllRooms() {
        return ctx.selectFrom(ROOM).orderBy(ROOM.ROOM_NUMBER).fetch();
    }

    public void saveRoomType(RoomTypeRecord roomType) {
        try {
            if (roomType.getId() == null) {
                ctx.insertInto(ROOM_TYPE).set(roomType).execute();
            } else {
                ctx.update(ROOM_TYPE).set(roomType).where(ROOM_TYPE.ID.eq(roomType.getId())).execute();
            }
        } catch (DuplicateKeyException e) {
            throw new DuplicateException(ROOM_TYPE_NAME_USED);
        }
    }

    public void saveRoom(RoomRecord room) {
        try {
            if (room.getId() == null) {
                ctx.insertInto(ROOM).set(room).execute();
            } else {
                ctx.update(ROOM).set(room).where(ROOM.ID.eq(room.getId())).execute();
            }
        } catch (DuplicateKeyException e) {
            throw new DuplicateException(ROOM_NUMBER_USED);
        }
    }

    /**
     * The name or number is already used; the message is meant for the user.
     */
    public static class DuplicateException extends RuntimeException {

        public DuplicateException(String message) {
            super(message);
        }
    }
}
