package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.room.RoomService;
import ch.martinelli.edu.hrs.room.RoomTypeService;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AnonymizationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private Clock clock;

    private LocalDate today;
    private long roomId;

    @BeforeEach
    void createRoom() {
        today = LocalDate.now(clock);
        var type = roomTypeService.save(roomType("Test Anonymize", 2, "100.00"));
        var room = new RoomRecord();
        room.setRoomNumber("T751");
        room.setRoomTypeId(type.getId());
        roomId = roomService.save(room).getId();
    }

    @Test
    void oldStaysAndCancellationsAreAnonymized() {
        var oldStay = stay(today.minusMonths(13), "CHECKED_OUT", null);
        var oldCancellation = stay(today.plusDays(30), "CANCELLED", today.minusMonths(13));
        var recentStay = stay(today.minusMonths(11), "CHECKED_OUT", null);

        reservationService.anonymizeOlderThan(12);

        for (var id : new long[]{oldStay.getId(), oldCancellation.getId()}) {
            var stored = dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(id));
            assertThat(stored.getGuestName()).isEqualTo("Anonymized");
            assertThat(stored.getGuestEmail()).isNull();
            assertThat(stored.getGuestPhone()).isNull();
            assertThat(stored.getAnonymized()).isTrue();
        }
        var kept = dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(oldStay.getId()));
        assertThat(kept.getStatus()).isEqualTo("CHECKED_OUT");
        assertThat(kept.getRoomId()).isEqualTo(roomId);
        assertThat(kept.getTotalPrice()).isEqualByComparingTo("200.00");
        assertThat(kept.getDepartureDate()).isEqualTo(today.minusMonths(13));

        var recent = dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(recentStay.getId()));
        assertThat(recent.getGuestName()).isEqualTo("Test Old Guest");
        assertThat(recent.getAnonymized()).isFalse();
    }

    @Test
    void anonymizedReservationIsNotFoundByGuestLookup() {
        var oldStay = stay(today.minusMonths(13), "CHECKED_OUT", null);

        reservationService.anonymizeOlderThan(12);

        assertThat(reservationService.findForGuest(oldStay.getReservationNumber(), "old@example.com")).isEmpty();
    }

    /**
     * Booking cannot create past stays, so the dates and status are set afterwards.
     */
    private ReservationRecord stay(LocalDate departure, String status, LocalDate cancelledOn) {
        var reservation = reservationService.book(new BookingRequest(roomId, today.plusDays(200),
                today.plusDays(202), 1, "Test Old Guest", "old@example.com", "+41 79 000 00 00",
                BookingSource.ONLINE));
        dsl.update(RESERVATION)
                .set(RESERVATION.ARRIVAL_DATE, departure.minusDays(2))
                .set(RESERVATION.DEPARTURE_DATE, departure)
                .set(RESERVATION.STATUS, status)
                .set(RESERVATION.CANCELLED_AT, cancelledOn == null ? null : cancelledOn.atStartOfDay())
                .where(RESERVATION.ID.eq(reservation.getId())).execute();
        return dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(reservation.getId()));
    }
}
