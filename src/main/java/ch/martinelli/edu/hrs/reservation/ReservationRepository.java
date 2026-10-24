package ch.martinelli.edu.hrs.reservation;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;

/**
 * Records reservations (UC-001 step 9).
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
}
