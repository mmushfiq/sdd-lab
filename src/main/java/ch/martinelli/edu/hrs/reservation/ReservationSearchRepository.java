package ch.martinelli.edu.hrs.reservation;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static ch.martinelli.edu.hrs.db.Tables.GUEST;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static org.jooq.impl.DSL.noCondition;

/**
 * Finds reservations for the receptionist (UC-007 steps 3 and 5).
 */
@Repository
public class ReservationSearchRepository {

    private final DSLContext ctx;

    public ReservationSearchRepository(DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Returns at most {@code limit} reservations matching all entered criteria, ordered by arrival date.
     */
    // UC-007 BR-002: Reservations of every status and channel are found.
    public List<ReservationSearchResult> search(ReservationSearchCriteria criteria, int limit) {
        return ctx.select(RESERVATION.RESERVATION_CODE, GUEST.FIRST_NAME, GUEST.LAST_NAME, RESERVATION.ARRIVAL_DATE,
                        RESERVATION.DEPARTURE_DATE, ROOM_TYPE.NAME, RESERVATION.NUMBER_OF_GUESTS,
                        RESERVATION.STATUS.convertFrom(ReservationStatus::valueOf))
                .from(RESERVATION)
                .join(GUEST).on(GUEST.ID.eq(RESERVATION.GUEST_ID))
                .join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(RESERVATION.ROOM_TYPE_ID))
                // UC-007 BR-001: Several criteria combine so that all of them must match.
                .where(guestNameMatches(criteria.guestName()))
                .and(reservationCodeMatches(criteria.reservationCode()))
                .and(dateMatches(criteria))
                .orderBy(RESERVATION.ARRIVAL_DATE, RESERVATION.RESERVATION_CODE)
                .limit(limit)
                .fetch(Records.mapping(ReservationSearchResult::new));
    }

    // UC-007 BR-001: The entered text is part of the first name or last name, compared case-insensitively.
    private static Condition guestNameMatches(String guestName) {
        if (ReservationSearchCriteria.isBlank(guestName)) {
            return noCondition();
        }
        String text = guestName.strip();
        return GUEST.FIRST_NAME.containsIgnoreCase(text).or(GUEST.LAST_NAME.containsIgnoreCase(text));
    }

    // UC-007 BR-001: The reservation code matches only exactly.
    private static Condition reservationCodeMatches(String reservationCode) {
        if (ReservationSearchCriteria.isBlank(reservationCode)) {
            return noCondition();
        }
        return RESERVATION.RESERVATION_CODE.eq(reservationCode.strip());
    }

    // UC-007 BR-001: Arrival date on or before the date and departure date on or after the date.
    private static Condition dateMatches(ReservationSearchCriteria criteria) {
        if (criteria.date() == null) {
            return noCondition();
        }
        return RESERVATION.ARRIVAL_DATE.le(criteria.date()).and(RESERVATION.DEPARTURE_DATE.ge(criteria.date()));
    }

    /**
     * Finds all data of the reservation with this reservation code, with the guest's contact data and the assigned
     * room.
     */
    public Optional<ReservationDetail> findDetail(String reservationCode) {
        return ctx.select(RESERVATION.RESERVATION_CODE, RESERVATION.STATUS.convertFrom(ReservationStatus::valueOf),
                        RESERVATION.ARRIVAL_DATE, RESERVATION.DEPARTURE_DATE, ROOM_TYPE.NAME,
                        RESERVATION.NUMBER_OF_GUESTS, RESERVATION.TOTAL_PRICE,
                        RESERVATION.CHANNEL.convertFrom(ReservationChannel::valueOf), RESERVATION.CREATED_AT,
                        RESERVATION.CHECKED_IN_AT, RESERVATION.CHECKED_OUT_AT, RESERVATION.CANCELLED_AT,
                        GUEST.FIRST_NAME, GUEST.LAST_NAME, GUEST.EMAIL, GUEST.PHONE, ROOM.ROOM_NUMBER)
                .from(RESERVATION)
                .join(GUEST).on(GUEST.ID.eq(RESERVATION.GUEST_ID))
                .join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(RESERVATION.ROOM_TYPE_ID))
                .leftJoin(ROOM).on(ROOM.ID.eq(RESERVATION.ROOM_ID))
                .where(RESERVATION.RESERVATION_CODE.eq(reservationCode))
                .fetchOptional(Records.mapping(ReservationDetail::new));
    }
}
