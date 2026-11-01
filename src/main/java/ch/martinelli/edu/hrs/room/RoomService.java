package ch.martinelli.edu.hrs.room;

import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import ch.martinelli.edu.hrs.db.tables.records.SeasonalRateRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static ch.martinelli.edu.hrs.db.Tables.SEASONAL_RATE;

/**
 * Maintains room types, rooms and seasonal rates. Records without an id are created, records with an id are updated.
 * Uniqueness of names and numbers is left to the database's unique constraints.
 */
@Service
@Transactional
public class RoomService {

    public static final String ROOM_TYPE_NAME_USED = "A room type with this name already exists";
    public static final String ROOM_NUMBER_USED = "A room with this number already exists";
    public static final String LAST_BEFORE_FIRST = "Last night cannot be before first night";
    private static final String SEASONS_OVERLAP = "The dates overlap the seasonal rate \"%s\" of this room type";

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
     * Seasonal rates sorted by room type name and first night.
     */
    @Transactional(readOnly = true)
    public List<SeasonalRateRecord> findAllSeasonalRates() {
        return ctx.select(SEASONAL_RATE.fields()).from(SEASONAL_RATE)
                .join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(SEASONAL_RATE.ROOM_TYPE_ID))
                .orderBy(ROOM_TYPE.NAME, SEASONAL_RATE.FIRST_NIGHT)
                .fetchInto(SEASONAL_RATE);
    }

    /**
     * Saves a seasonal rate unless its last night is before its first night or it overlaps another seasonal rate of
     * the same room type. The room type row is locked first, so two overlapping saves are checked one after the other.
     */
    public void saveSeasonalRate(SeasonalRateRecord rate) {
        if (rate.getLastNight().isBefore(rate.getFirstNight())) {
            throw new SeasonalRateException(LAST_BEFORE_FIRST);
        }
        ctx.selectFrom(ROOM_TYPE).where(ROOM_TYPE.ID.eq(rate.getRoomTypeId())).forUpdate().fetchSingle();
        Condition overlap = SEASONAL_RATE.ROOM_TYPE_ID.eq(rate.getRoomTypeId())
                .and(SEASONAL_RATE.FIRST_NIGHT.le(rate.getLastNight()))
                .and(SEASONAL_RATE.LAST_NIGHT.ge(rate.getFirstNight()));
        if (rate.getId() != null) {
            overlap = overlap.and(SEASONAL_RATE.ID.ne(rate.getId()));
        }
        ctx.select(SEASONAL_RATE.NAME).from(SEASONAL_RATE).where(overlap).orderBy(SEASONAL_RATE.FIRST_NIGHT).limit(1)
                .fetchOptional(SEASONAL_RATE.NAME)
                .ifPresent(name -> {
                    throw new SeasonalRateException(SEASONS_OVERLAP.formatted(name));
                });
        if (rate.getId() == null) {
            ctx.insertInto(SEASONAL_RATE).set(rate).execute();
        } else {
            ctx.update(SEASONAL_RATE).set(rate).where(SEASONAL_RATE.ID.eq(rate.getId())).execute();
        }
    }

    public void deleteSeasonalRate(long id) {
        ctx.deleteFrom(SEASONAL_RATE).where(SEASONAL_RATE.ID.eq(id)).execute();
    }

    /**
     * A seasonal rate breaks a date rule; the message is meant for the user.
     */
    public static class SeasonalRateException extends RuntimeException {

        public SeasonalRateException(String message) {
            super(message);
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
