package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;

/**
 * Creates, changes and cancels reservations. Saving locks the room row first, so two bookings of the same room are
 * checked for overlap one after the other, also when they are saved at the same moment.
 */
@Service
@Transactional
public class ReservationService {

    public static final String BOOKED = "BOOKED";
    public static final String CANCELLED = "CANCELLED";

    public static final String ROOM_NOT_AVAILABLE = "The room is no longer available for these dates";
    public static final String TOO_MANY_GUESTS = "Too many guests for this room";
    public static final String NOT_BOOKED = "Only Booked reservations can be changed or cancelled";
    public static final String GUEST_NAME_REQUIRED = "Guest name is required";
    public static final String EMAIL_REQUIRED = "Email is required";
    public static final String NOT_FOUND = "No reservation found for this reservation number and email";
    public static final String CONTACT_HOTEL = "Please contact the hotel to cancel on or after the arrival date";

    static final String NUMBER_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int NUMBER_LENGTH = 8;
    private static final int NUMBER_ATTEMPTS = 3;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DSLContext ctx;
    private final AvailabilityService availabilityService;
    private final Clock clock;

    public ReservationService(DSLContext ctx, AvailabilityService availabilityService, Clock clock) {
        this.ctx = ctx;
        this.availabilityService = availabilityService;
        this.clock = clock;
    }

    public record Stay(long roomId, LocalDate arrival, LocalDate departure, Integer guests) {
    }

    public record Guest(String name, String email, String phone) {
    }

    public record GuestReservation(ReservationRecord reservation, String roomTypeName) {
    }

    /**
     * Saves a Booked reservation at the room type's current nightly rate and returns it with its reservation number.
     */
    public ReservationRecord create(Stay stay, Guest guest) {
        if (guest.name() == null || guest.name().isBlank()) {
            throw new ReservationException(GUEST_NAME_REQUIRED);
        }
        if (guest.email() == null || guest.email().isBlank()) {
            throw new ReservationException(EMAIL_REQUIRED);
        }
        RoomTypeRecord roomType = checkStay(stay, null);
        ReservationRecord reservation = ctx.newRecord(RESERVATION);
        reservation.setRoomId(stay.roomId());
        reservation.setArrivalDate(stay.arrival());
        reservation.setDepartureDate(stay.departure());
        reservation.setNumberOfGuests(stay.guests());
        reservation.setGuestName(guest.name().strip());
        reservation.setGuestEmail(guest.email().strip());
        reservation.setGuestPhone(guest.phone() == null || guest.phone().isBlank() ? null : guest.phone().strip());
        reservation.setNightlyRate(roomType.getNightlyRate());
        reservation.setStatus(BOOKED);
        for (int attempt = 1; ; attempt++) {
            reservation.setReservationNumber(newReservationNumber());
            try {
                reservation.insert();
                return reservation;
            } catch (DuplicateKeyException e) {
                if (attempt == NUMBER_ATTEMPTS) {
                    throw e;
                }
            }
        }
    }

    /**
     * Changes dates, room and guests of a Booked reservation. The nightly rate stays unless the room type changes;
     * then it becomes the new room type's current rate.
     */
    public ReservationRecord change(long reservationId, Stay stay) {
        ReservationRecord reservation = findBooked(reservationId);
        RoomTypeRecord newType = checkStay(stay, reservationId);
        Long oldTypeId = ctx.select(ROOM.ROOM_TYPE_ID).from(ROOM).where(ROOM.ID.eq(reservation.getRoomId()))
                .fetchSingle(ROOM.ROOM_TYPE_ID);
        if (!newType.getId().equals(oldTypeId)) {
            reservation.setNightlyRate(newType.getNightlyRate());
        }
        reservation.setRoomId(stay.roomId());
        reservation.setArrivalDate(stay.arrival());
        reservation.setDepartureDate(stay.departure());
        reservation.setNumberOfGuests(stay.guests());
        reservation.update();
        return reservation;
    }

    public void cancel(long reservationId) {
        ReservationRecord reservation = findBooked(reservationId);
        reservation.setStatus(CANCELLED);
        reservation.update();
    }

    /**
     * The reservation with this number and booking email, both matched ignoring case and surrounding spaces. An
     * unknown number and a wrong email both give an empty result.
     */
    @Transactional(readOnly = true)
    public Optional<GuestReservation> findForGuest(String reservationNumber, String email) {
        if (reservationNumber == null || email == null) {
            return Optional.empty();
        }
        return ctx.select(RESERVATION, ROOM_TYPE.NAME).from(RESERVATION)
                .join(ROOM).on(ROOM.ID.eq(RESERVATION.ROOM_ID))
                .join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(ROOM.ROOM_TYPE_ID))
                // CHAR column would bind the input as padded CHAR(8), so compare on VARCHAR
                .where(RESERVATION.RESERVATION_NUMBER.cast(SQLDataType.VARCHAR(8))
                        .eq(reservationNumber.strip().toUpperCase(Locale.ROOT)))
                .and(DSL.lower(RESERVATION.GUEST_EMAIL).eq(email.strip().toLowerCase(Locale.ROOT)))
                .fetchOptional(r -> new GuestReservation(r.value1(), r.value2()));
    }

    /**
     * A guest may cancel only a Booked reservation, and only before its arrival date.
     */
    public boolean guestMayCancel(ReservationRecord reservation) {
        return BOOKED.equals(reservation.getStatus()) && LocalDate.now(clock).isBefore(reservation.getArrivalDate());
    }

    public void cancelByGuest(String reservationNumber, String email) {
        ReservationRecord reservation = findForGuest(reservationNumber, email)
                .orElseThrow(() -> new ReservationException(NOT_FOUND)).reservation();
        if (!BOOKED.equals(reservation.getStatus())) {
            throw new ReservationException(NOT_BOOKED);
        }
        if (!guestMayCancel(reservation)) {
            throw new ReservationException(CONTACT_HOTEL);
        }
        cancel(reservation.getId());
    }

    /**
     * Reservations departing today or later, sorted by arrival date, optionally filtered by a part of the
     * reservation number or guest name.
     */
    @Transactional(readOnly = true)
    public List<ReservationRecord> findUpcoming(String search) {
        Condition condition = RESERVATION.DEPARTURE_DATE.ge(LocalDate.now(clock));
        if (search != null && !search.isBlank()) {
            // CHAR column would bind the search text as padded CHAR(8), so search on VARCHAR
            condition = condition.and(RESERVATION.RESERVATION_NUMBER.cast(SQLDataType.VARCHAR(8))
                    .containsIgnoreCase(search.strip())
                    .or(RESERVATION.GUEST_NAME.containsIgnoreCase(search.strip())));
        }
        return ctx.selectFrom(RESERVATION).where(condition)
                .orderBy(RESERVATION.ARRIVAL_DATE, RESERVATION.ID).fetch();
    }

    /**
     * Validates the stay against the locked room and returns the room's type.
     */
    private RoomTypeRecord checkStay(Stay stay, Long excludedReservationId) {
        availabilityService.validate(stay.arrival(), stay.departure(), stay.guests());
        ctx.selectFrom(ROOM).where(ROOM.ID.eq(stay.roomId())).forUpdate().fetchSingle();
        RoomTypeRecord roomType = ctx.select(ROOM_TYPE.fields()).from(ROOM_TYPE)
                .join(ROOM).on(ROOM.ROOM_TYPE_ID.eq(ROOM_TYPE.ID))
                .where(ROOM.ID.eq(stay.roomId()))
                .fetchSingleInto(ROOM_TYPE);
        if (stay.guests() > roomType.getCapacity()) {
            throw new ReservationException(TOO_MANY_GUESTS);
        }
        Condition overlap = RESERVATION.ROOM_ID.eq(stay.roomId())
                .and(AvailabilityService.overlaps(stay.arrival(), stay.departure()));
        if (excludedReservationId != null) {
            overlap = overlap.and(RESERVATION.ID.ne(excludedReservationId));
        }
        if (ctx.fetchExists(RESERVATION, overlap)) {
            throw new ReservationException(ROOM_NOT_AVAILABLE);
        }
        return roomType;
    }

    private ReservationRecord findBooked(long reservationId) {
        ReservationRecord reservation = ctx.fetchSingle(RESERVATION, RESERVATION.ID.eq(reservationId));
        if (!BOOKED.equals(reservation.getStatus())) {
            throw new ReservationException(NOT_BOOKED);
        }
        return reservation;
    }

    private static String newReservationNumber() {
        StringBuilder number = new StringBuilder(NUMBER_LENGTH);
        for (int i = 0; i < NUMBER_LENGTH; i++) {
            number.append(NUMBER_CHARACTERS.charAt(RANDOM.nextInt(NUMBER_CHARACTERS.length())));
        }
        return number.toString();
    }
}
