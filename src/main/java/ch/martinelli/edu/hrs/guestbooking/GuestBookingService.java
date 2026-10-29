package ch.martinelli.edu.hrs.guestbooking;

import ch.martinelli.edu.hrs.reservation.AvailableRoom;
import ch.martinelli.edu.hrs.reservation.Guest;
import ch.martinelli.edu.hrs.reservation.ReservationNotModifiableException;
import ch.martinelli.edu.hrs.reservation.ReservationService;
import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import ch.martinelli.edu.hrs.reservation.RoomNotAvailableException;
import ch.martinelli.edu.hrs.reservation.StayPeriod;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.jooq.impl.DSL.lower;
import static org.jooq.impl.DSL.upper;

/**
 * The public guest operations. Searching, booking and cancelling go through {@link ReservationService}; only the
 * guest lookup reads the tables directly (as in ADR-002).
 */
@Service
public class GuestBookingService {

    private final ReservationService reservationService;
    private final DSLContext dsl;
    private final Clock clock;

    public GuestBookingService(ReservationService reservationService, DSLContext dsl, Clock clock) {
        this.reservationService = reservationService;
        this.dsl = dsl;
        this.clock = clock;
    }

    /**
     * Free rooms grouped by room type, in the order of each type's lowest free room number.
     */
    public List<RoomTypeAvailability> searchByRoomType(StayPeriod period, int numberOfGuests) {
        return reservationService.searchAvailability(period, numberOfGuests).stream()
                .collect(groupingBy(AvailableRoom::roomTypeName, LinkedHashMap::new, toList()))
                .values().stream()
                .map(rooms -> {
                    var room = rooms.getFirst();
                    return new RoomTypeAvailability(room.roomTypeName(), room.capacity(), room.nightlyRate(),
                            room.totalPrice(), rooms.size());
                })
                .toList();
    }

    /**
     * Books the free room of the type with the lowest number. Deliberately not transactional: each attempt is its
     * own locked transaction in {@link ReservationService#createReservation}, so a room taken meanwhile does not
     * roll back the attempt on the next one.
     *
     * @throws RoomTypeNotAvailableException if no room of the type is free anymore
     */
    public GuestReservation bookRoomType(String roomTypeName, StayPeriod period, int numberOfGuests, Guest guest) {
        for (var room : reservationService.searchAvailability(period, numberOfGuests)) {
            if (!room.roomTypeName().equals(roomTypeName)) {
                continue;
            }
            try {
                var bookingNumber = reservationService.createReservation(room.roomId(), period, numberOfGuests,
                        guest);
                return new GuestReservation(bookingNumber, period.arrival(), period.departure(), roomTypeName,
                        numberOfGuests, room.totalPrice(), ReservationStatus.CONFIRMED);
            } catch (RoomNotAvailableException _) {
                // booked by someone else since the search: try the next room of the type
            }
        }
        throw new RoomTypeNotAvailableException();
    }

    /**
     * Booking number (trimmed) and email match case-insensitively. Anonymized reservations never match.
     */
    @Transactional(readOnly = true)
    public Optional<GuestReservation> findByBookingNumberAndEmail(String bookingNumber, String email) {
        return dsl.select(RESERVATION.BOOKING_NUMBER, RESERVATION.ARRIVAL, RESERVATION.DEPARTURE, ROOM_TYPE.NAME,
                        RESERVATION.NUMBER_OF_GUESTS, RESERVATION.TOTAL_PRICE, RESERVATION.STATUS)
                .from(RESERVATION)
                .join(ROOM).on(ROOM.ID.eq(RESERVATION.ROOM_ID))
                .join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(ROOM.ROOM_TYPE_ID))
                .where(upper(RESERVATION.BOOKING_NUMBER).eq(bookingNumber.trim().toUpperCase()))
                .and(lower(RESERVATION.GUEST_EMAIL).eq(email.trim().toLowerCase()))
                .and(RESERVATION.ANONYMIZED.isFalse())
                .fetchOptional(r -> new GuestReservation(r.value1(), r.value2(), r.value3(), r.value4(), r.value5(),
                        r.value6(), ReservationStatus.valueOf(r.value7())));
    }

    /**
     * Whether the guest may still cancel online now.
     */
    public boolean canCancel(GuestReservation reservation) {
        return reservation.status() == ReservationStatus.CONFIRMED
                && reservation.beforeCancellationDeadline(clock.instant());
    }

    /**
     * Checks the deadline again at confirm time, the page may have been open since before it.
     *
     * @throws CancellationDeadlinePassedException if it is 14:00 or later on the day before arrival
     * @throws ReservationNotModifiableException   if there is no such reservation or it is not confirmed
     */
    @Transactional
    public void cancelByGuest(String bookingNumber, String email) {
        var reservation = findByBookingNumberAndEmail(bookingNumber, email)
                .orElseThrow(() -> new ReservationNotModifiableException("Reservation can no longer be cancelled"));
        if (!reservation.beforeCancellationDeadline(clock.instant())) {
            throw new CancellationDeadlinePassedException();
        }
        reservationService.cancelReservation(reservation.bookingNumber());
    }
}
