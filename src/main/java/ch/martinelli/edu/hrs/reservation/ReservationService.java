package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.notExists;
import static org.jooq.impl.DSL.selectOne;
import static org.jooq.impl.DSL.upper;
import static org.jooq.impl.DSL.val;

/**
 * Availability search, booking, the reservation list and the reservation lifecycle (change, cancel, anonymize).
 * Reads the room catalog tables directly (ADR-002) and locks the room row when booking (ADR-001).
 */
@Service
@Transactional
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    // no 0/O and no 1/I, so booking numbers can be read out on the phone without confusion
    private static final String BOOKING_NUMBER_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int BOOKING_NUMBER_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static final String ANONYMIZED = "Anonymized";

    private final DSLContext dsl;

    public ReservationService(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * In-service rooms with enough capacity and no overlapping confirmed reservation, sorted by room number.
     */
    @Transactional(readOnly = true)
    public List<AvailableRoom> searchAvailability(StayPeriod period, int numberOfGuests) {
        requireNewStay(period, numberOfGuests);
        var nights = BigDecimal.valueOf(period.nights());
        return dsl.select(ROOM.ID, ROOM.NUMBER, ROOM_TYPE.NAME, ROOM_TYPE.CAPACITY, ROOM_TYPE.NIGHTLY_RATE)
                .from(ROOM)
                .join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(ROOM.ROOM_TYPE_ID))
                .where(ROOM.OUT_OF_SERVICE.isFalse())
                .and(ROOM_TYPE.CAPACITY.ge(numberOfGuests))
                .and(notExists(confirmedOverlapping(ROOM.ID, period)))
                .orderBy(ROOM.NUMBER)
                .fetch(r -> new AvailableRoom(r.value1(), r.value2(), r.value3(), r.value4(), r.value5(),
                        r.value5().multiply(nights)));
    }

    /**
     * Books the room as a CONFIRMED reservation priced at the current nightly rate.
     *
     * @return the new booking number
     * @throws RoomNotAvailableException  if the room is gone, out of service or already booked for any of the nights
     * @throws CapacityExceededException if there are more guests than the room allows
     */
    public String createReservation(long roomId, StayPeriod period, int numberOfGuests, Guest guest) {
        requireNewStay(period, numberOfGuests);
        var roomType = lockBookableRoom(roomId, period, numberOfGuests, null,
                RoomNotAvailableException.NO_LONGER_AVAILABLE);

        var bookingNumber = newBookingNumber();
        dsl.insertInto(RESERVATION)
                .set(RESERVATION.BOOKING_NUMBER, bookingNumber)
                .set(RESERVATION.ROOM_ID, roomId)
                .set(RESERVATION.ARRIVAL, period.arrival())
                .set(RESERVATION.DEPARTURE, period.departure())
                .set(RESERVATION.NUMBER_OF_GUESTS, numberOfGuests)
                .set(RESERVATION.GUEST_NAME, guest.name())
                .set(RESERVATION.GUEST_EMAIL, guest.email())
                .set(RESERVATION.GUEST_PHONE, guest.phone())
                .set(RESERVATION.TOTAL_PRICE, price(roomType, period))
                .set(RESERVATION.STATUS, ReservationStatus.CONFIRMED.name())
                .execute();
        // no guest data in the log
        log.info("Reservation {} created for room {} from {} to {}", bookingNumber, roomId, period.arrival(),
                period.departure());
        return bookingNumber;
    }

    /**
     * Applies new dates, room, number of guests and guest data. The price is recalculated with the current rate
     * only when the room or the dates change. A staying guest keeps the arrival and may move the departure.
     *
     * @throws ReservationNotModifiableException if the reservation is cancelled, anonymized or departed
     * @throws RoomNotAvailableException         if the room is out of service or booked by another reservation
     * @throws CapacityExceededException        if there are more guests than the room allows
     */
    public void changeReservation(String bookingNumber, long roomId, StayPeriod period, int numberOfGuests,
                                  Guest guest) {
        var id = lockReservation(bookingNumber);
        var current = id == null ? null : findItems(RESERVATION.ID.eq(id)).getFirst();
        if (current == null || !current.changeable()) {
            throw new ReservationNotModifiableException("Reservation can no longer be changed");
        }
        var today = LocalDate.now();
        if (!period.arrival().equals(current.arrival()) && period.arrival().isBefore(today)) {
            throw new IllegalArgumentException("Arrival must not be in the past");
        }
        if (period.departure().isBefore(today)) {
            throw new IllegalArgumentException("Departure must not be in the past");
        }
        if (numberOfGuests < 1) {
            throw new IllegalArgumentException("At least one guest is required");
        }
        var roomType = lockBookableRoom(roomId, period, numberOfGuests, id, RoomNotAvailableException.NOT_AVAILABLE);

        var repriced = roomId != current.roomId() || !period.equals(current.period());
        dsl.update(RESERVATION)
                .set(RESERVATION.ROOM_ID, roomId)
                .set(RESERVATION.ARRIVAL, period.arrival())
                .set(RESERVATION.DEPARTURE, period.departure())
                .set(RESERVATION.NUMBER_OF_GUESTS, numberOfGuests)
                .set(RESERVATION.GUEST_NAME, guest.name())
                .set(RESERVATION.GUEST_EMAIL, guest.email())
                .set(RESERVATION.GUEST_PHONE, guest.phone())
                .set(RESERVATION.TOTAL_PRICE, repriced ? price(roomType, period) : current.totalPrice())
                .where(RESERVATION.ID.eq(id))
                .execute();
        log.info("Reservation {} changed: room {} from {} to {}, price recalculated: {}", bookingNumber, roomId,
                period.arrival(), period.departure(), repriced);
    }

    /**
     * @throws ReservationNotModifiableException if the reservation is not confirmed
     */
    public void cancelReservation(String bookingNumber) {
        var id = lockReservation(bookingNumber);
        if (id == null || !findItems(RESERVATION.ID.eq(id)).getFirst().cancellable()) {
            throw new ReservationNotModifiableException("Reservation can no longer be cancelled");
        }
        dsl.update(RESERVATION)
                .set(RESERVATION.STATUS, ReservationStatus.CANCELLED.name())
                .where(RESERVATION.ID.eq(id))
                .execute();
        log.info("Reservation {} cancelled", bookingNumber);
    }

    /**
     * Irreversibly replaces name, email and phone with {@value #ANONYMIZED} (ADR-003).
     *
     * @throws ReservationNotModifiableException if the reservation is confirmed and not departed, or already
     *                                           anonymized
     */
    public void anonymizeGuest(String bookingNumber) {
        var id = lockReservation(bookingNumber);
        if (id == null || !findItems(RESERVATION.ID.eq(id)).getFirst().anonymizable()) {
            throw new ReservationNotModifiableException("Reservation cannot be anonymized");
        }
        dsl.update(RESERVATION)
                .set(RESERVATION.GUEST_NAME, ANONYMIZED)
                .set(RESERVATION.GUEST_EMAIL, ANONYMIZED)
                .set(RESERVATION.GUEST_PHONE, ANONYMIZED)
                .set(RESERVATION.ANONYMIZED, true)
                .where(RESERVATION.ID.eq(id))
                .execute();
        // never log the removed data
        log.info("Guest data of reservation {} anonymized", bookingNumber);
    }

    /**
     * Without criteria: confirmed reservations departing today or later. A booking number matches exactly that
     * reservation in any status. A date range [from, to] (both inclusive) replaces the "today or later" default.
     */
    @Transactional(readOnly = true)
    public List<ReservationListItem> findReservations(ReservationFilter filter) {
        Condition condition;
        if (hasText(filter.bookingNumber())) {
            condition = upper(RESERVATION.BOOKING_NUMBER).eq(filter.bookingNumber().trim().toUpperCase());
        } else {
            condition = filter.includeCancelled() ? noCondition()
                    : RESERVATION.STATUS.eq(ReservationStatus.CONFIRMED.name());
            if (hasText(filter.guestName())) {
                condition = condition.and(RESERVATION.GUEST_NAME.containsIgnoreCase(filter.guestName().trim()));
            }
            if (filter.from() == null && filter.to() == null) {
                condition = condition.and(RESERVATION.DEPARTURE.ge(LocalDate.now()));
            }
            if (filter.from() != null) {
                condition = condition.and(RESERVATION.DEPARTURE.gt(filter.from()));
            }
            if (filter.to() != null) {
                condition = condition.and(RESERVATION.ARRIVAL.le(filter.to()));
            }
        }
        return findItems(condition);
    }

    private List<ReservationListItem> findItems(Condition condition) {
        return dsl.select(RESERVATION.BOOKING_NUMBER, RESERVATION.GUEST_NAME, RESERVATION.GUEST_EMAIL,
                        RESERVATION.GUEST_PHONE, ROOM.ID, ROOM.NUMBER, RESERVATION.ARRIVAL, RESERVATION.DEPARTURE,
                        RESERVATION.NUMBER_OF_GUESTS, RESERVATION.TOTAL_PRICE, RESERVATION.STATUS,
                        RESERVATION.ANONYMIZED)
                .from(RESERVATION)
                .join(ROOM).on(ROOM.ID.eq(RESERVATION.ROOM_ID))
                .where(condition)
                .orderBy(RESERVATION.ARRIVAL, RESERVATION.BOOKING_NUMBER)
                .fetch(r -> new ReservationListItem(r.value1(), r.value2(), r.value3(), r.value4(), r.value5(),
                        r.value6(), r.value7(), r.value8(), r.value9(), r.value10(),
                        ReservationStatus.valueOf(r.value11()), r.value12()));
    }

    /**
     * Locks the reservation row against concurrent lifecycle changes (ADR-001: reservation row before room row).
     *
     * @return the reservation id, or null if there is no such reservation
     */
    private Long lockReservation(String bookingNumber) {
        return dsl.select(RESERVATION.ID)
                .from(RESERVATION)
                .where(RESERVATION.BOOKING_NUMBER.eq(bookingNumber))
                .forUpdate()
                .fetchOne(RESERVATION.ID);
    }

    /**
     * Locks the room row (ADR-001), then checks it is in service, big enough and free for the period, ignoring the
     * reservation being changed.
     */
    private RoomTypeRecord lockBookableRoom(long roomId, StayPeriod period, int numberOfGuests,
                                            Long ownReservationId, String unavailableMessage) {
        var room = dsl.select(ROOM.OUT_OF_SERVICE, ROOM.ROOM_TYPE_ID)
                .from(ROOM)
                .where(ROOM.ID.eq(roomId))
                .forUpdate()
                .fetchOne();
        if (room == null || room.value1()) {
            throw new RoomNotAvailableException(unavailableMessage);
        }
        var roomType = dsl.fetchSingle(ROOM_TYPE, ROOM_TYPE.ID.eq(room.value2()));
        if (numberOfGuests > roomType.getCapacity()) {
            throw new CapacityExceededException(roomType.getCapacity());
        }
        var overlapping = confirmedOverlapping(val(roomId), period)
                .and(ownReservationId == null ? noCondition() : RESERVATION.ID.ne(ownReservationId));
        // count, not exists: H2 can return a stale EXISTS result after an uncommitted write in the same transaction
        if (dsl.fetchCount(overlapping) > 0) {
            throw new RoomNotAvailableException(unavailableMessage);
        }
        return roomType;
    }

    private static BigDecimal price(RoomTypeRecord roomType, StayPeriod period) {
        return roomType.getNightlyRate().multiply(BigDecimal.valueOf(period.nights()));
    }

    private SelectConditionStep<Record1<Integer>> confirmedOverlapping(Field<Long> roomId, StayPeriod period) {
        return selectOne()
                .from(RESERVATION)
                .where(RESERVATION.ROOM_ID.eq(roomId))
                .and(RESERVATION.STATUS.eq(ReservationStatus.CONFIRMED.name()))
                .and(period.overlaps(RESERVATION.ARRIVAL, RESERVATION.DEPARTURE));
    }

    private String newBookingNumber() {
        String bookingNumber;
        do {
            bookingNumber = RANDOM.ints(BOOKING_NUMBER_LENGTH, 0, BOOKING_NUMBER_ALPHABET.length())
                    .mapToObj(i -> String.valueOf(BOOKING_NUMBER_ALPHABET.charAt(i)))
                    .collect(Collectors.joining());
        } while (dsl.fetchCount(RESERVATION, RESERVATION.BOOKING_NUMBER.eq(bookingNumber)) > 0);
        return bookingNumber;
    }

    // the views validate first; this guards the service boundary
    private static void requireNewStay(StayPeriod period, int numberOfGuests) {
        if (period.arrival().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Arrival must not be in the past");
        }
        if (numberOfGuests < 1) {
            throw new IllegalArgumentException("At least one guest is required");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
