package ch.martinelli.edu.hrs.reservation;

import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static ch.martinelli.edu.hrs.db.Tables.GUEST;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;

/**
 * Records reservations (UC-001 step 9) and finds a guest's own reservation (UC-002 step 3).
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
                .and(GUEST.EMAIL.notLike("%@" + GuestRepository.ERASED_EMAIL_DOMAIN))
                .fetchOptional(Records.mapping(OwnReservation::new));
    }
}
