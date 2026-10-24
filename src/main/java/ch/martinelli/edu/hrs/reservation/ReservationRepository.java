package ch.martinelli.edu.hrs.reservation;

import org.jooq.DSLContext;
import org.jooq.Record12;
import org.jooq.Record7;
import org.jooq.Records;
import org.jooq.SelectConditionStep;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static ch.martinelli.edu.hrs.db.Tables.GUEST;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;

/**
 * Records reservations (UC-001 step 9), finds and cancels a guest's own reservation (UC-002 step 3,
 * UC-003 step 9), changes a reservation (UC-005 steps 2 and 7) and checks it in (UC-009 step 7).
 */
@Repository
public class ReservationRepository {

    private final DSLContext ctx;

    public ReservationRepository(DSLContext ctx) {
        this.ctx = ctx;
    }

    public boolean existsByReservationCode(String reservationCode) {
        return ctx.fetchExists(RESERVATION, RESERVATION.RESERVATION_CODE.eq(reservationCode));
    }

    public void insert(String reservationCode, Long guestId, Long roomTypeId, LocalDate arrivalDate,
                       LocalDate departureDate, int numberOfGuests, BigDecimal totalPrice,
                       ReservationStatus status, ReservationChannel channel, LocalDateTime createdAt) {
        ctx.insertInto(RESERVATION)
                .set(RESERVATION.RESERVATION_CODE, reservationCode)
                .set(RESERVATION.GUEST_ID, guestId)
                .set(RESERVATION.ROOM_TYPE_ID, roomTypeId)
                .set(RESERVATION.ARRIVAL_DATE, arrivalDate)
                .set(RESERVATION.DEPARTURE_DATE, departureDate)
                .set(RESERVATION.NUMBER_OF_GUESTS, numberOfGuests)
                .set(RESERVATION.TOTAL_PRICE, totalPrice)
                .set(RESERVATION.STATUS, status.name())
                .set(RESERVATION.CHANNEL, channel.name())
                .set(RESERVATION.CREATED_AT, createdAt)
                .execute();
    }

    /**
     * Finds the reservation with this reservation code whose guest has this email address. The email address must
     * already be normalised to lower case.
     */
    public Optional<OwnReservation> findOwnReservation(String reservationCode, String normalisedEmail) {
        return ownReservationQuery(reservationCode, normalisedEmail)
                .fetchOptional(Records.mapping(OwnReservation::new));
    }

    /**
     * Like {@link #findOwnReservation(String, String)}, and locks the reservation until the transaction ends.
     */
    public Optional<OwnReservation> findOwnReservationForUpdate(String reservationCode, String normalisedEmail) {
        return ownReservationQuery(reservationCode, normalisedEmail)
                .forUpdate().of(RESERVATION)
                .fetchOptional(Records.mapping(OwnReservation::new));
    }

    private SelectConditionStep<Record7<String, ReservationStatus, LocalDate, LocalDate, String, Integer,
            BigDecimal>> ownReservationQuery(String reservationCode, String normalisedEmail) {
        return ctx.select(RESERVATION.RESERVATION_CODE, RESERVATION.STATUS.convertFrom(ReservationStatus::valueOf),
                        RESERVATION.ARRIVAL_DATE, RESERVATION.DEPARTURE_DATE, ROOM_TYPE.NAME,
                        RESERVATION.NUMBER_OF_GUESTS, RESERVATION.TOTAL_PRICE)
                .from(RESERVATION)
                .join(GUEST).on(GUEST.ID.eq(RESERVATION.GUEST_ID))
                .join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(RESERVATION.ROOM_TYPE_ID))
                // UC-002 BR-001: The reservation code matches exactly; reservations of every status are found.
                .where(RESERVATION.RESERVATION_CODE.eq(reservationCode))
                // UC-002 BR-001: The email address matches the guest's, case-insensitively (stored in lower case).
                .and(GUEST.EMAIL.eq(normalisedEmail))
                // UC-002 BR-004: A guest whose data was erased can no longer be found.
                .and(GUEST.EMAIL.notLike("%@" + GuestRepository.ERASED_EMAIL_DOMAIN));
    }

    /**
     * Records the reservation with status Cancelled and the time of cancellation (UC-003 step 9).
     */
    public void cancel(String reservationCode, LocalDateTime cancelledAt) {
        ctx.update(RESERVATION)
                .set(RESERVATION.STATUS, ReservationStatus.CANCELLED.name())
                .set(RESERVATION.CANCELLED_AT, cancelledAt)
                .where(RESERVATION.RESERVATION_CODE.eq(reservationCode))
                .execute();
    }

    /**
     * Finds the reservation with this code as the receptionist changes it (UC-005 step 2).
     */
    public Optional<ChangeableReservation> findChangeable(String reservationCode) {
        return changeableQuery(reservationCode).fetchOptional(Records.mapping(ChangeableReservation::new));
    }

    /**
     * Like {@link #findChangeable(String)}, and locks the reservation until the transaction ends (UC-005 step 7).
     */
    public Optional<ChangeableReservation> findChangeableForUpdate(String reservationCode) {
        return changeableQuery(reservationCode).forUpdate().of(RESERVATION)
                .fetchOptional(Records.mapping(ChangeableReservation::new));
    }

    private SelectConditionStep<Record12<Long, String, ReservationStatus, LocalDate, LocalDate, Long,
            String, Integer, BigDecimal, Long, String, String>> changeableQuery(String reservationCode) {
        return ctx.select(RESERVATION.ID, RESERVATION.RESERVATION_CODE,
                        RESERVATION.STATUS.convertFrom(ReservationStatus::valueOf), RESERVATION.ARRIVAL_DATE,
                        RESERVATION.DEPARTURE_DATE, RESERVATION.ROOM_TYPE_ID, ROOM_TYPE.NAME,
                        RESERVATION.NUMBER_OF_GUESTS, RESERVATION.TOTAL_PRICE, RESERVATION.ROOM_ID, ROOM.ROOM_NUMBER,
                        GUEST.EMAIL)
                .from(RESERVATION)
                .join(GUEST).on(GUEST.ID.eq(RESERVATION.GUEST_ID))
                .join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(RESERVATION.ROOM_TYPE_ID))
                .leftJoin(ROOM).on(ROOM.ID.eq(RESERVATION.ROOM_ID))
                .where(RESERVATION.RESERVATION_CODE.eq(reservationCode));
    }

    /**
     * Records the new dates, room type, number of guests and total price; code, guest, channel and assigned room stay
     * (UC-005 step 7).
     */
    public void change(Long reservationId, LocalDate arrivalDate, LocalDate departureDate, Long roomTypeId,
                       int numberOfGuests, BigDecimal totalPrice) {
        ctx.update(RESERVATION)
                .set(RESERVATION.ARRIVAL_DATE, arrivalDate)
                .set(RESERVATION.DEPARTURE_DATE, departureDate)
                .set(RESERVATION.ROOM_TYPE_ID, roomTypeId)
                .set(RESERVATION.NUMBER_OF_GUESTS, numberOfGuests)
                .set(RESERVATION.TOTAL_PRICE, totalPrice)
                .where(RESERVATION.ID.eq(reservationId))
                .execute();
    }

    /**
     * Records the reservation with status Checked In, the assigned room and the time of check-in (UC-009 step 7).
     */
    public void checkIn(Long reservationId, Long roomId, LocalDateTime checkedInAt) {
        ctx.update(RESERVATION)
                .set(RESERVATION.STATUS, ReservationStatus.CHECKED_IN.name())
                .set(RESERVATION.ROOM_ID, roomId)
                .set(RESERVATION.CHECKED_IN_AT, checkedInAt)
                .where(RESERVATION.ID.eq(reservationId))
                .execute();
    }
}
