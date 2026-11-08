package ch.martinelli.edu.hrs.room;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import org.jooq.DSLContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static ch.martinelli.edu.hrs.core.BusinessRuleException.check;
import static ch.martinelli.edu.hrs.db.Sequences.ROOM_SEQ;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;

@Service
@Transactional
public class RoomService {

    private final DSLContext dsl;

    public RoomService(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Transactional(readOnly = true)
    public List<RoomRecord> findAll() {
        return dsl.selectFrom(ROOM).orderBy(ROOM.ROOM_NUMBER).fetch();
    }

    /**
     * Inserts a new room (cleaning status clean) or updates an existing one.
     */
    public RoomRecord save(RoomRecord room) {
        check(room.getRoomNumber() != null && !room.getRoomNumber().isBlank(), "Room number is required.");
        check(room.getRoomTypeId() != null, "Room type is required.");
        var numberUsed = 0 < dsl.fetchCount(ROOM, ROOM.ROOM_NUMBER.eq(room.getRoomNumber())
                .and(room.getId() == null ? ROOM.ID.isNotNull() : ROOM.ID.ne(room.getId())));
        if (numberUsed) {
            throw new BusinessRuleException("Room number is already used.");
        }
        if (room.getId() == null) {
            room.setId(dsl.nextval(ROOM_SEQ));
            room.setCleaningStatus(CleaningStatus.CLEAN.name());
            dsl.executeInsert(room);
        } else {
            dsl.update(ROOM)
                    .set(ROOM.ROOM_NUMBER, room.getRoomNumber())
                    .set(ROOM.ROOM_TYPE_ID, room.getRoomTypeId())
                    .where(ROOM.ID.eq(room.getId()))
                    .execute();
        }
        return room;
    }

    /**
     * Rooms with any reservation, also past ones, cannot be deleted so reports stay correct (research R8).
     */
    public void delete(long id) {
        try {
            dsl.deleteFrom(ROOM).where(ROOM.ID.eq(id)).execute();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleException("Room has reservations and cannot be deleted.");
        }
    }
}
