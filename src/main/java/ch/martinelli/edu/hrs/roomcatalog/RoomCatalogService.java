package ch.martinelli.edu.hrs.roomcatalog;

import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static org.jooq.impl.DSL.lower;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.val;

/**
 * Room types and rooms. Uniqueness and usage rules are checked here; the database constraints are the safety net.
 */
@Service
@Transactional
public class RoomCatalogService {

    private final DSLContext dsl;

    public RoomCatalogService(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Transactional(readOnly = true)
    public List<RoomTypeRecord> findRoomTypes() {
        return dsl.selectFrom(ROOM_TYPE).orderBy(ROOM_TYPE.NAME).fetch();
    }

    /**
     * Inserts the room type when its id is null, otherwise updates it.
     *
     * @throws RoomTypeNameExistsException if another room type has the same name, ignoring case
     * @throws RoomTypeNotFoundException   if the room type to update no longer exists
     */
    public void saveRoomType(RoomTypeRecord roomType) {
        roomType.setName(roomType.getName().trim());
        if (dsl.fetchExists(ROOM_TYPE, ROOM_TYPE.NAME_LOWER.eq(lower(val(roomType.getName())))
                .and(roomType.getId() == null ? noCondition() : ROOM_TYPE.ID.ne(roomType.getId())))) {
            throw new RoomTypeNameExistsException();
        }
        if (roomType.getId() == null) {
            dsl.executeInsert(roomType);
        } else if (dsl.executeUpdate(roomType) == 0) {
            throw new RoomTypeNotFoundException();
        }
    }

    /**
     * @throws RoomTypeInUseException    if a room still uses the room type
     * @throws RoomTypeNotFoundException if the room type no longer exists
     */
    public void deleteRoomType(long id) {
        if (dsl.fetchExists(ROOM, ROOM.ROOM_TYPE_ID.eq(id))) {
            throw new RoomTypeInUseException();
        }
        if (dsl.deleteFrom(ROOM_TYPE).where(ROOM_TYPE.ID.eq(id)).execute() == 0) {
            throw new RoomTypeNotFoundException();
        }
    }

    @Transactional(readOnly = true)
    public List<RoomListItem> findRooms() {
        return findRooms(noCondition());
    }

    /**
     * Rooms that can be offered for a stay: in service and with at least the given capacity, sorted by number.
     */
    @Transactional(readOnly = true)
    public List<RoomListItem> findBookableRooms(int minCapacity) {
        return findRooms(ROOM.OUT_OF_SERVICE.isFalse().and(ROOM_TYPE.CAPACITY.ge(minCapacity)));
    }

    /**
     * Inserts the room when its id is null, otherwise updates it.
     *
     * @throws RoomNumberExistsException if another room has the same number
     */
    public void saveRoom(RoomRecord room) {
        room.setNumber(room.getNumber().trim());
        if (dsl.fetchExists(ROOM, ROOM.NUMBER.eq(room.getNumber())
                .and(room.getId() == null ? noCondition() : ROOM.ID.ne(room.getId())))) {
            throw new RoomNumberExistsException();
        }
        if (room.getId() == null) {
            dsl.executeInsert(room);
        } else {
            dsl.executeUpdate(room);
        }
    }

    public void deleteRoom(long id) {
        dsl.deleteFrom(ROOM).where(ROOM.ID.eq(id)).execute();
    }

    private List<RoomListItem> findRooms(Condition condition) {
        return dsl.select(ROOM.ID, ROOM.NUMBER, ROOM_TYPE.ID, ROOM_TYPE.NAME, ROOM_TYPE.CAPACITY,
                        ROOM_TYPE.NIGHTLY_RATE, ROOM.OUT_OF_SERVICE)
                .from(ROOM)
                .join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(ROOM.ROOM_TYPE_ID))
                .where(condition)
                .orderBy(ROOM.NUMBER)
                .fetch(r -> new RoomListItem(r.value1(), r.value2(), r.value3(), r.value4(), r.value5(),
                        r.value6(), r.value7()));
    }
}
