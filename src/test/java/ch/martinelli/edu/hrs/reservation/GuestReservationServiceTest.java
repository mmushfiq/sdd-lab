package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class GuestReservationServiceTest {

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
        var type = roomTypeService.save(roomType("Test Guest Double", 2, "100.00"));
        var room = new RoomRecord();
        room.setRoomNumber("T741");
        room.setRoomTypeId(type.getId());
        roomId = roomService.save(room).getId();
    }

    @Test
    void lookupByNumberAndEmailIgnoringEmailCase() {
        var reservation = book(today.plusDays(3));

        var found = reservationService.findForGuest(reservation.getReservationNumber().toLowerCase(),
                " Guest.Online@Example.com ");

        assertThat(found).hasValueSatisfying(g -> {
            assertThat(g.reservationNumber()).isEqualTo(reservation.getReservationNumber());
            assertThat(g.arrival()).isEqualTo(today.plusDays(3));
            assertThat(g.roomTypeName()).isEqualTo("Test Guest Double");
            assertThat(g.totalPrice()).isEqualByComparingTo("200.00");
            assertThat(g.status()).isEqualTo(ReservationStatus.BOOKED);
            assertThat(g.cancellable()).isTrue();
        });
    }

    @Test
    void wrongDataFindsNothingAndCancelGivesTheSameMessage() {
        var reservation = book(today.plusDays(3));

        assertThat(reservationService.findForGuest(reservation.getReservationNumber(), "other@example.com"))
                .isEmpty();
        assertThat(reservationService.findForGuest("ZZZZZZZZ", "guest.online@example.com")).isEmpty();
        assertThatThrownBy(() -> reservationService.cancelByGuest(reservation.getReservationNumber(),
                "other@example.com")).hasMessage(ReservationService.NOT_FOUND);
        assertThatThrownBy(() -> reservationService.cancelByGuest("ZZZZZZZZ", "guest.online@example.com"))
                .hasMessage(ReservationService.NOT_FOUND);
    }

    @Test
    void cancelBeforeArrivalDay() {
        var reservation = book(today.plusDays(1));

        reservationService.cancelByGuest(reservation.getReservationNumber(), "guest.online@example.com");

        var stored = dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(reservation.getId()));
        assertThat(stored.getStatus()).isEqualTo(ReservationStatus.CANCELLED.name());
        assertThat(stored.getCancelledAt()).isNotNull();
    }

    @Test
    void cancelOnArrivalDayAsksToCallReception() {
        var reservation = book(today);

        assertThat(reservationService.findForGuest(reservation.getReservationNumber(), "guest.online@example.com"))
                .hasValueSatisfying(g -> assertThat(g.cancellable()).isFalse());
        assertThatThrownBy(() -> reservationService.cancelByGuest(reservation.getReservationNumber(),
                "guest.online@example.com"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Online cancellation is only possible until the day before arrival. Please call reception.");
    }

    @Test
    void notBookedCannotBeCancelled() {
        var checkedIn = book(today);
        reservationService.checkIn(checkedIn.getId());

        assertThatThrownBy(() -> reservationService.cancelByGuest(checkedIn.getReservationNumber(),
                "guest.online@example.com"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Only a booked reservation can be cancelled.");
    }

    private ReservationRecord book(LocalDate arrival) {
        return reservationService.book(new BookingRequest(roomId, arrival, arrival.plusDays(2), 2,
                "Test Online Guest", "guest.online@example.com", "+41 79 000 00 00", BookingSource.ONLINE));
    }
}
