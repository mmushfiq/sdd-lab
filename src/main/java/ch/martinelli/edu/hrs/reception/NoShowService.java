package ch.martinelli.edu.hrs.reception;

import ch.martinelli.edu.hrs.reservation.ChangeableReservation;
import ch.martinelli.edu.hrs.reservation.ReservationRepository;
import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.GUEST;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;

/**
 * UC-012 Record No-Show: lists the missed arrivals and records a no-show for one of them. The system never records a
 * no-show on its own (UC-012 BR-002).
 */
@Service
public class NoShowService {

    private final DSLContext ctx;
    private final ReservationRepository reservationRepository;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public NoShowService(DSLContext ctx, ReservationRepository reservationRepository,
                         TransactionTemplate transactionTemplate, Clock clock) {
        this.ctx = ctx;
        this.reservationRepository = reservationRepository;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    /**
     * Step 2: every Confirmed reservation whose arrival date is before today, by arrival date.
     */
    public List<MissedArrival> missedArrivals() {
        return ctx.select(RESERVATION.RESERVATION_CODE, GUEST.FIRST_NAME, GUEST.LAST_NAME, GUEST.PHONE,
                        RESERVATION.ARRIVAL_DATE, RESERVATION.DEPARTURE_DATE, ROOM_TYPE.NAME)
                .from(RESERVATION)
                .join(GUEST).on(GUEST.ID.eq(RESERVATION.GUEST_ID))
                .join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(RESERVATION.ROOM_TYPE_ID))
                // UC-012 BR-001: Status Confirmed and arrival date before today in the hotel's local time.
                .where(RESERVATION.STATUS.eq(ReservationStatus.CONFIRMED.name()))
                .and(RESERVATION.ARRIVAL_DATE.lt(LocalDate.now(clock)))
                .orderBy(RESERVATION.ARRIVAL_DATE, GUEST.LAST_NAME, RESERVATION.RESERVATION_CODE)
                .fetch(Records.mapping(MissedArrival::new));
    }

    /**
     * Steps 7 and 8, A4: checks status and arrival date again and records the no-show.
     *
     * @throws NoShowNotPossibleException when the reservation is no longer a missed arrival (A4)
     */
    public void recordNoShow(String reservationCode) {
        transactionTemplate.executeWithoutResult(_ -> {
            ChangeableReservation reservation = reservationRepository.findChangeableForUpdate(reservationCode)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown reservation " + reservationCode));
            // UC-012 BR-001: Only Confirmed with an arrival date before today.
            if (reservation.status() != ReservationStatus.CONFIRMED
                    || !reservation.arrivalDate().isBefore(LocalDate.now(clock))) {
                throw new NoShowNotPossibleException(reservation.status());
            }
            // UC-012 BR-003: No fee, no payment and no email. The dates, room type and total price are kept.
            reservationRepository.recordNoShow(reservation.id());
        });
    }
}
