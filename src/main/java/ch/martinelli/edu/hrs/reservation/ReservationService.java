package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.room.CleaningStatus;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static ch.martinelli.edu.hrs.core.BusinessRuleException.check;
import static ch.martinelli.edu.hrs.db.Sequences.RESERVATION_SEQ;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;

@Service
@Transactional
public class ReservationService {

    static final int ANONYMIZE_AFTER_MONTHS = 12;
    /** 002 FR-002: hours before check-in for every reservation booked from 002 on (old rows have 15, V4). */
    static final int ONLINE_CANCELLATION_HOURS = 48;
    public static final String NOT_FOUND = "No reservation was found for this reservation number and email.";

    private final DSLContext dsl;
    private final AvailabilityService availabilityService;
    private final ReservationNumberGenerator numberGenerator;
    private final Clock clock;

    public ReservationService(DSLContext dsl, AvailabilityService availabilityService,
                              ReservationNumberGenerator numberGenerator, Clock clock) {
        this.dsl = dsl;
        this.availabilityService = availabilityService;
        this.numberGenerator = numberGenerator;
        this.clock = clock;
    }

    /**
     * Books a room. The room row is locked first, so two bookings of the same room run one after the other and
     * the second one sees the first in its overlap check (FR-007, research R3).
     */
    public ReservationRecord book(BookingRequest request) {
        check(request.source() != null, "Booking source is required.");
        var reservation = dsl.newRecord(RESERVATION);
        reservation.setId(dsl.nextval(RESERVATION_SEQ));
        reservation.setReservationNumber(newReservationNumber());
        apply(request, reservation, null, true);
        reservation.setStatus(ReservationStatus.BOOKED.name());
        reservation.setSource(request.source().name());
        reservation.setCancellationHoursBefore(ONLINE_CANCELLATION_HOURS);
        reservation.setCreatedAt(LocalDateTime.now(clock));
        reservation.insert();
        return reservation;
    }

    /**
     * Finds reservations by exact reservation number or by part of the guest name (FR-013).
     */
    @Transactional(readOnly = true)
    public List<FoundReservation> find(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        var text = query.trim();
        return dsl.select(RESERVATION, ROOM.ROOM_NUMBER)
                .from(RESERVATION).join(ROOM).on(ROOM.ID.eq(RESERVATION.ROOM_ID))
                .where(RESERVATION.RESERVATION_NUMBER.eq(text.toUpperCase())
                        .or(RESERVATION.GUEST_NAME.containsIgnoreCase(text)))
                .orderBy(RESERVATION.ARRIVAL_DATE, RESERVATION.RESERVATION_NUMBER)
                .fetch(r -> new FoundReservation(r.value1(), r.value2()));
    }

    /**
     * Changes dates, room, number of guests or guest details of a booked reservation (FR-013, FR-016). A new
     * room or new dates are priced with the current rate (US6/AC1); otherwise the booked rate and total stay
     * (US1/AC5). The source of the request is ignored.
     */
    public ReservationRecord change(long reservationId, BookingRequest request) {
        var reservation = lock(reservationId);
        check(ReservationStatus.BOOKED.name().equals(reservation.getStatus()),
                "Only a booked reservation can be changed.");
        check(request.arrival() != null && request.departure() != null, "Arrival and departure are required.");
        var reprice = request.roomId() != reservation.getRoomId()
                || !request.arrival().equals(reservation.getArrivalDate())
                || !request.departure().equals(reservation.getDepartureDate());
        apply(request, reservation, reservationId, reprice);
        reservation.update();
        return reservation;
    }

    /**
     * Cancels a booked reservation; its nights are free again (FR-014, FR-016).
     */
    public void cancel(long reservationId) {
        var reservation = lock(reservationId);
        check(ReservationStatus.BOOKED.name().equals(reservation.getStatus()),
                "Only a booked reservation can be cancelled.");
        reservation.setStatus(ReservationStatus.CANCELLED.name());
        reservation.setCancelledAt(LocalDateTime.now(clock));
        reservation.update();
    }

    /**
     * A guest's own reservation by reservation number and booking email (FR-015). Email case does not matter;
     * an anonymized reservation has no email any more and cannot be found.
     */
    @Transactional(readOnly = true)
    public Optional<GuestReservation> findForGuest(String reservationNumber, String email) {
        if (reservationNumber == null || email == null) {
            return Optional.empty();
        }
        var now = nowInHotel();
        return dsl.select(RESERVATION.RESERVATION_NUMBER, RESERVATION.ARRIVAL_DATE, RESERVATION.DEPARTURE_DATE,
                        ROOM_TYPE.NAME, RESERVATION.TOTAL_PRICE, RESERVATION.STATUS,
                        RESERVATION.CANCELLATION_HOURS_BEFORE)
                .from(RESERVATION)
                .join(ROOM).on(ROOM.ID.eq(RESERVATION.ROOM_ID))
                .join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(ROOM.ROOM_TYPE_ID))
                .where(guest(reservationNumber, email))
                .fetchOptional(r -> {
                    var status = ReservationStatus.valueOf(r.value6());
                    var deadline = CancellationDeadline.of(r.value2(), r.value7());
                    return new GuestReservation(r.value1(), r.value2(), r.value3(), r.value4(), r.value5(), status,
                            status == ReservationStatus.BOOKED && deadline.allowsOnlineCancellationAt(now),
                            deadline);
                });
    }

    /**
     * 002 FR-004 (replaces 001 FR-015): a guest can cancel online only before the reservation's online
     * cancellation deadline; afterwards only reception can.
     */
    public void cancelByGuest(String reservationNumber, String email) {
        check(reservationNumber != null && email != null, NOT_FOUND);
        var reservation = dsl.selectFrom(RESERVATION).where(guest(reservationNumber, email)).forUpdate()
                .fetchOptional()
                .orElseThrow(() -> new BusinessRuleException(NOT_FOUND));
        check(ReservationStatus.BOOKED.name().equals(reservation.getStatus()),
                "Only a booked reservation can be cancelled.");
        var deadline = CancellationDeadline.of(reservation.getArrivalDate(), reservation.getCancellationHoursBefore());
        check(deadline.allowsOnlineCancellationAt(nowInHotel()), deadline.passedText());
        reservation.setStatus(ReservationStatus.CANCELLED.name());
        reservation.setCancelledAt(LocalDateTime.now(clock));
        reservation.update();
    }

    /**
     * FR-026: removes guest name, email and phone of reservations whose departure or cancellation is more than
     * the given months ago; dates, room, prices and status stay for the reports. Returns how many were changed.
     */
    public int anonymizeOlderThan(int months) {
        var cutoff = LocalDate.now(clock).minusMonths(months);
        return dsl.update(RESERVATION)
                .set(RESERVATION.GUEST_NAME, "Anonymized")
                .setNull(RESERVATION.GUEST_EMAIL)
                .setNull(RESERVATION.GUEST_PHONE)
                .set(RESERVATION.ANONYMIZED, true)
                .where(RESERVATION.ANONYMIZED.isFalse())
                .and(RESERVATION.DEPARTURE_DATE.lt(cutoff)
                        .or(RESERVATION.CANCELLED_AT.lt(cutoff.atStartOfDay())))
                .execute();
    }

    private LocalDateTime nowInHotel() {
        return LocalDateTime.now(clock.withZone(CancellationDeadline.ZONE));
    }

    private static Condition guest(String reservationNumber, String email) {
        return RESERVATION.RESERVATION_NUMBER.eq(reservationNumber.trim().toUpperCase())
                .and(RESERVATION.GUEST_EMAIL.equalIgnoreCase(email.trim()));
    }

    /**
     * Checks the stay and guest, locks the room and checks it is free, then copies the request into the record
     * with the current rate. The room lock serializes bookings of one room (FR-007, research R3).
     */
    private void apply(BookingRequest request, ReservationRecord reservation, Long ownId, boolean reprice) {
        availabilityService.checkStay(request.arrival(), request.departure(), request.numberOfGuests());
        checkGuest(request.guestName(), request.guestEmail(), request.guestPhone());

        var room = dsl.select(ROOM.ID, ROOM_TYPE.CAPACITY, ROOM_TYPE.NIGHTLY_RATE)
                .from(ROOM).join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(ROOM.ROOM_TYPE_ID))
                .where(ROOM.ID.eq(request.roomId()))
                .forUpdate().of(ROOM)
                .fetchOptional()
                .orElseThrow(() -> new BusinessRuleException("The room does not exist."));
        check(request.numberOfGuests() <= room.value2(), "The room is too small for this number of guests.");
        var others = ownId == null ? RESERVATION.ID.isNotNull() : RESERVATION.ID.ne(ownId);
        var taken = dsl.fetchCount(RESERVATION, RESERVATION.ROOM_ID.eq(request.roomId()).and(others)
                .and(AvailabilityService.blocks(request.arrival(), request.departure()))) > 0;
        check(!taken, "The room is no longer available for these dates.");

        var nightlyRate = room.value3();
        reservation.setRoomId(request.roomId());
        reservation.setArrivalDate(request.arrival());
        reservation.setDepartureDate(request.departure());
        reservation.setNumberOfGuests(request.numberOfGuests());
        reservation.setGuestName(request.guestName().trim());
        reservation.setGuestEmail(request.guestEmail().trim());
        reservation.setGuestPhone(request.guestPhone().trim());
        if (reprice) {
            reservation.setNightlyRate(nightlyRate);
            reservation.setTotalPrice(nightlyRate.multiply(
                    BigDecimal.valueOf(AvailabilityService.nights(request.arrival(), request.departure()))));
        }
    }

    /**
     * FR-017: only a booked reservation, on its arrival date, into a clean room.
     */
    public void checkIn(long reservationId) {
        var reservation = lock(reservationId);
        check(ReservationStatus.BOOKED.name().equals(reservation.getStatus()),
                "Only a booked reservation can be checked in.");
        check(reservation.getArrivalDate().equals(LocalDate.now(clock)),
                "Check-in is only possible on the arrival date.");
        var cleaningStatus = dsl.select(ROOM.CLEANING_STATUS).from(ROOM)
                .where(ROOM.ID.eq(reservation.getRoomId())).forUpdate().fetchSingle().value1();
        check(CleaningStatus.CLEAN.name().equals(cleaningStatus), "The room is not clean yet.");
        reservation.setStatus(ReservationStatus.CHECKED_IN.name());
        reservation.update();
    }

    /**
     * FR-018: check-out ends the stay and marks the room dirty for Housekeeping.
     */
    public void checkOut(long reservationId) {
        var reservation = lock(reservationId);
        check(ReservationStatus.CHECKED_IN.name().equals(reservation.getStatus()),
                "Only a checked-in reservation can be checked out.");
        reservation.setStatus(ReservationStatus.CHECKED_OUT.name());
        reservation.update();
        dsl.update(ROOM).set(ROOM.CLEANING_STATUS, CleaningStatus.DIRTY.name())
                .where(ROOM.ID.eq(reservation.getRoomId())).execute();
    }

    @Transactional(readOnly = true)
    public List<Stay> findArrivals(LocalDate today) {
        return findStays(RESERVATION.STATUS.eq(ReservationStatus.BOOKED.name())
                .and(RESERVATION.ARRIVAL_DATE.eq(today)));
    }

    /**
     * Checked-in stays departing today, plus overdue ones not yet checked out (FR-018).
     */
    @Transactional(readOnly = true)
    public List<Stay> findDepartures(LocalDate today) {
        return findStays(RESERVATION.STATUS.eq(ReservationStatus.CHECKED_IN.name())
                .and(RESERVATION.DEPARTURE_DATE.le(today)));
    }

    /**
     * FR-019 (data-model.md): OCCUPIED if a checked-in guest is in the room, else BOOKED if a booked
     * reservation has a night today, else FREE. A checked-in guest stays OCCUPIED until check-out, also on or
     * after the departure date.
     */
    @Transactional(readOnly = true)
    public List<RoomStatus> roomOverview(LocalDate today) {
        var occupied = dsl.select(RESERVATION.ROOM_ID).from(RESERVATION)
                .where(RESERVATION.STATUS.eq(ReservationStatus.CHECKED_IN.name()))
                .and(RESERVATION.ARRIVAL_DATE.le(today))
                .fetchSet(RESERVATION.ROOM_ID);
        var booked = dsl.select(RESERVATION.ROOM_ID).from(RESERVATION)
                .where(AvailabilityService.blocks(today, today.plusDays(1)))
                .fetchSet(RESERVATION.ROOM_ID);
        return dsl.select(ROOM.ID, ROOM.ROOM_NUMBER, ROOM_TYPE.NAME, ROOM.CLEANING_STATUS)
                .from(ROOM).join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(ROOM.ROOM_TYPE_ID))
                .orderBy(ROOM.ROOM_NUMBER)
                .fetch(r -> new RoomStatus(r.value2(), r.value3(),
                        occupied.contains(r.value1()) ? Occupancy.OCCUPIED
                                : booked.contains(r.value1()) ? Occupancy.BOOKED : Occupancy.FREE,
                        r.value4()));
    }

    private List<Stay> findStays(Condition condition) {
        return dsl.select(RESERVATION.ID, RESERVATION.RESERVATION_NUMBER, RESERVATION.GUEST_NAME,
                        ROOM.ROOM_NUMBER, RESERVATION.ARRIVAL_DATE, RESERVATION.DEPARTURE_DATE,
                        RESERVATION.NUMBER_OF_GUESTS)
                .from(RESERVATION).join(ROOM).on(ROOM.ID.eq(RESERVATION.ROOM_ID))
                .where(condition)
                .orderBy(ROOM.ROOM_NUMBER)
                .fetch(r -> new Stay(r.value1(), r.value2(), r.value3(), r.value4(), r.value5(), r.value6(),
                        r.value7()));
    }

    private ReservationRecord lock(long reservationId) {
        return dsl.selectFrom(RESERVATION).where(RESERVATION.ID.eq(reservationId)).forUpdate()
                .fetchOptional()
                .orElseThrow(() -> new BusinessRuleException("The reservation does not exist."));
    }

    private String newReservationNumber() {
        String number;
        do {
            number = numberGenerator.next();
        } while (dsl.fetchCount(RESERVATION, RESERVATION.RESERVATION_NUMBER.eq(number)) > 0);
        return number;
    }

    private static void checkGuest(String name, String email, String phone) {
        check(name != null && !name.isBlank(), "Guest name is required.");
        check(email != null && email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"), "A valid email is required.");
        check(phone != null && !phone.isBlank(), "Phone is required.");
    }
}
