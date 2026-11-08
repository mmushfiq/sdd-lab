package ch.martinelli.edu.hrs.room;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static ch.martinelli.edu.hrs.db.Sequences.ROOM_TYPE_SEQ;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;

@Service
@Transactional
public class RoomTypeService {

    private final DSLContext dsl;

    public RoomTypeService(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Transactional(readOnly = true)
    public List<RoomTypeRecord> findAll() {
        return dsl.selectFrom(ROOM_TYPE).orderBy(ROOM_TYPE.NAME).fetch();
    }

    /**
     * Inserts a new room type or updates an existing one. Reservations keep the rate they were booked with.
     */
    public RoomTypeRecord save(RoomTypeRecord roomType) {
        // fetchCount, not fetchExists: H2 answered EXISTS on this varchar comparison wrongly (false for an existing row)
        var nameUsed = 0 < dsl.fetchCount(ROOM_TYPE, ROOM_TYPE.NAME.eq(roomType.getName())
                .and(roomType.getId() == null ? ROOM_TYPE.ID.isNotNull() : ROOM_TYPE.ID.ne(roomType.getId())));
        if (nameUsed) {
            throw new BusinessRuleException("Room type name is already used.");
        }
        if (roomType.getId() == null) {
            roomType.setId(dsl.nextval(ROOM_TYPE_SEQ));
            dsl.executeInsert(roomType);
        } else {
            dsl.executeUpdate(roomType);
        }
        return roomType;
    }

    public void delete(long id) {
        if (dsl.fetchExists(ROOM, ROOM.ROOM_TYPE_ID.eq(id))) {
            throw new BusinessRuleException("Room type is still in use.");
        }
        dsl.deleteFrom(ROOM_TYPE).where(ROOM_TYPE.ID.eq(id)).execute();
    }
}
