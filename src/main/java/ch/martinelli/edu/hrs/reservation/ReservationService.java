package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

import static ch.martinelli.edu.hrs.core.BusinessRuleException.check;
import static ch.martinelli.edu.hrs.db.Sequences.RESERVATION_SEQ;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;

@Service
@Transactional
public class ReservationService {

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
        availabilityService.checkStay(request.arrival(), request.departure(), request.numberOfGuests());
        checkGuest(request.guestName(), request.guestEmail(), request.guestPhone());
        check(request.source() != null, "Booking source is required.");

        var room = dsl.select(ROOM.ID, ROOM_TYPE.CAPACITY, ROOM_TYPE.NIGHTLY_RATE)
                .from(ROOM).join(ROOM_TYPE).on(ROOM_TYPE.ID.eq(ROOM.ROOM_TYPE_ID))
                .where(ROOM.ID.eq(request.roomId()))
                .forUpdate().of(ROOM)
                .fetchOptional()
                .orElseThrow(() -> new BusinessRuleException("The room does not exist."));
        check(request.numberOfGuests() <= room.value2(), "The room is too small for this number of guests.");
        var taken = dsl.fetchCount(RESERVATION, RESERVATION.ROOM_ID.eq(request.roomId())
                .and(AvailabilityService.blocks(request.arrival(), request.departure()))) > 0;
        check(!taken, "The room is no longer available for these dates.");

        var nightlyRate = room.value3();
        var reservation = dsl.newRecord(RESERVATION);
        reservation.setId(dsl.nextval(RESERVATION_SEQ));
        reservation.setReservationNumber(newReservationNumber());
        reservation.setRoomId(request.roomId());
        reservation.setArrivalDate(request.arrival());
        reservation.setDepartureDate(request.departure());
        reservation.setNumberOfGuests(request.numberOfGuests());
        reservation.setGuestName(request.guestName().trim());
        reservation.setGuestEmail(request.guestEmail().trim());
        reservation.setGuestPhone(request.guestPhone().trim());
        reservation.setNightlyRate(nightlyRate);
        reservation.setTotalPrice(nightlyRate.multiply(
                BigDecimal.valueOf(AvailabilityService.nights(request.arrival(), request.departure()))));
        reservation.setStatus(ReservationStatus.BOOKED.name());
        reservation.setSource(request.source().name());
        reservation.setCreatedAt(LocalDateTime.now(clock));
        reservation.insert();
        return reservation;
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
