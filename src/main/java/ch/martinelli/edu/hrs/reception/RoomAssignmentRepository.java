package ch.martinelli.edu.hrs.reception;

import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_BLOCK;
import static org.jooq.impl.DSL.exists;
import static org.jooq.impl.DSL.not;
import static org.jooq.impl.DSL.selectOne;

/**
 * Finds the rooms a reservation can be checked in to (UC-009 steps 4 and 6).
 */
@Repository
public class RoomAssignmentRepository {

    private static final String CLEAN = "CLEAN";

    private final DSLContext ctx;

    public RoomAssignmentRepository(DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * The free rooms of the room type for the stay from {@code arrivalDate} to the night before
     * {@code departureDate}, clean and dirty, ordered by room number.
     */
    public List<RoomCandidate> findFreeRooms(Long roomTypeId, LocalDate arrivalDate, LocalDate departureDate) {
        return ctx.select(ROOM.ID, ROOM.ROOM_NUMBER, ROOM.CLEANING_STATUS.eq(CLEAN))
                .from(ROOM)
                .where(isFree(roomTypeId, arrivalDate, departureDate))
                .orderBy(ROOM.ROOM_NUMBER)
                .fetch(Records.mapping(RoomCandidate::new));
    }

    /**
     * Locks the room until the end of the current transaction and tells whether it can still be assigned.
     */
    // UC-009 BR-003: The check and the recording are one indivisible action; the room row is locked.
    public boolean lockAndCheckAssignable(Long roomId, Long roomTypeId, LocalDate arrivalDate,
                                          LocalDate departureDate) {
        ctx.select(ROOM.ID).from(ROOM).where(ROOM.ID.eq(roomId)).forUpdate().fetchOptional();
        Optional<Long> assignable = ctx.select(ROOM.ID)
                .from(ROOM)
                .where(ROOM.ID.eq(roomId))
                .and(isFree(roomTypeId, arrivalDate, departureDate))
                // UC-009 BR-002: The room has cleaning status Clean.
                .and(ROOM.CLEANING_STATUS.eq(CLEAN))
                .fetchOptional(ROOM.ID);
        return assignable.isPresent();
    }

    // UC-009 BR-002: Active, of the reserved room type, not assigned to another Checked In reservation, and not
    // blocked on any night of the reservation.
    private static Condition isFree(Long roomTypeId, LocalDate arrivalDate, LocalDate departureDate) {
        return ROOM.ACTIVE.isTrue()
                .and(ROOM.ROOM_TYPE_ID.eq(roomTypeId))
                .and(not(exists(selectOne()
                        .from(RESERVATION)
                        .where(RESERVATION.ROOM_ID.eq(ROOM.ID))
                        .and(RESERVATION.STATUS.eq(ReservationStatus.CHECKED_IN.name())))))
                .and(not(exists(selectOne()
                        .from(ROOM_BLOCK)
                        .where(ROOM_BLOCK.ROOM_ID.eq(ROOM.ID))
                        .and(ROOM_BLOCK.START_DATE.le(departureDate.minusDays(1)))
                        .and(ROOM_BLOCK.END_DATE.ge(arrivalDate)))));
    }
}
