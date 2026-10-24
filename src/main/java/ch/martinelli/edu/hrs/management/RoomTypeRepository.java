package ch.martinelli.edu.hrs.management;

import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.select;

/**
 * Reads and records room types (UC-016 steps 2 and 7).
 */
@Repository
public class RoomTypeRepository {

    private final DSLContext ctx;

    public RoomTypeRepository(DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * All room types, also the inactive ones, ordered by name.
     */
    public List<RoomTypeOverview> findAll() {
        return ctx.select(ROOM_TYPE.ID, ROOM_TYPE.NAME, ROOM_TYPE.DESCRIPTION, ROOM_TYPE.MAX_GUESTS,
                        ROOM_TYPE.NIGHTLY_RATE, ROOM_TYPE.ACTIVE,
                        coalesce(field(select(count()).from(ROOM)
                                .where(ROOM.ROOM_TYPE_ID.eq(ROOM_TYPE.ID))
                                .and(ROOM.ACTIVE.isTrue())), 0))
                .from(ROOM_TYPE)
                .orderBy(ROOM_TYPE.NAME)
                .fetch(Records.mapping(RoomTypeOverview::new));
    }

    public boolean existsOtherWithName(String name, Long excludedId) {
        return ctx.fetchExists(ROOM_TYPE, ROOM_TYPE.NAME.eq(name)
                .and(excludedId == null ? ROOM_TYPE.ID.isNotNull() : ROOM_TYPE.ID.ne(excludedId)));
    }

    /**
     * Locks the room type until the end of the transaction; reservations lock the same row (UC-001 BR-005), so no
     * reservation with more guests can be recorded while the maximum is checked and saved.
     */
    public Optional<Long> lock(Long roomTypeId) {
        return ctx.select(ROOM_TYPE.ID).from(ROOM_TYPE).where(ROOM_TYPE.ID.eq(roomTypeId)).forUpdate()
                .fetchOptional(ROOM_TYPE.ID);
    }

    /**
     * The Confirmed and Checked In reservations of the room type with more guests than the maximum.
     */
    public List<AffectedReservation> findReservationsAbove(Long roomTypeId, int maxGuests) {
        return ctx.select(RESERVATION.RESERVATION_CODE, RESERVATION.NUMBER_OF_GUESTS)
                .from(RESERVATION)
                .where(RESERVATION.ROOM_TYPE_ID.eq(roomTypeId))
                .and(RESERVATION.STATUS.in(ReservationStatus.CONFIRMED.name(), ReservationStatus.CHECKED_IN.name()))
                .and(RESERVATION.NUMBER_OF_GUESTS.gt(maxGuests))
                .orderBy(RESERVATION.ARRIVAL_DATE, RESERVATION.RESERVATION_CODE)
                .fetch(Records.mapping(AffectedReservation::new));
    }

    public Long insert(RoomTypeData data) {
        return ctx.insertInto(ROOM_TYPE)
                .set(ROOM_TYPE.NAME, data.name())
                .set(ROOM_TYPE.DESCRIPTION, data.description())
                .set(ROOM_TYPE.MAX_GUESTS, data.maxGuests())
                .set(ROOM_TYPE.NIGHTLY_RATE, data.nightlyRate())
                .set(ROOM_TYPE.ACTIVE, data.active())
                .returning(ROOM_TYPE.ID)
                .fetchSingle(ROOM_TYPE.ID);
    }

    // UC-016 BR-002: Only the room type changes; the total prices of existing reservations stay.
    public void update(Long roomTypeId, RoomTypeData data) {
        ctx.update(ROOM_TYPE)
                .set(ROOM_TYPE.NAME, data.name())
                .set(ROOM_TYPE.DESCRIPTION, data.description())
                .set(ROOM_TYPE.MAX_GUESTS, data.maxGuests())
                .set(ROOM_TYPE.NIGHTLY_RATE, data.nightlyRate())
                .set(ROOM_TYPE.ACTIVE, data.active())
                .where(ROOM_TYPE.ID.eq(roomTypeId))
                .execute();
    }
}
