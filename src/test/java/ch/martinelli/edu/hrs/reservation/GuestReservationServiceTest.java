package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.room.RoomService;
import ch.martinelli.edu.hrs.room.RoomTypeService;
import ch.martinelli.edu.hrs.room.SeasonalRateService;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION_NIGHT;
import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static ch.martinelli.edu.hrs.room.SeasonalRateServiceTest.rate;
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
    private SeasonalRateService seasonalRateService;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private Clock clock;

    private LocalDate today;
    private long roomId;
    private long typeId;

    @BeforeEach
    void createRoom() {
        today = LocalDate.now(clock);
        var type = roomTypeService.save(roomType("Test Guest Double", 2, "100.00"));
        typeId = type.getId();
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
    void everyNewBookingGetsThe48HourRule() {
        var online = book(today.plusDays(5));
        var reception = reservationService.book(new BookingRequest(roomId, today.plusDays(10), today.plusDays(12),
                2, "Test Phone Guest", "guest.phone@example.com", "+41 79 000 00 01", BookingSource.RECEPTION));

        assertThat(hoursBefore(online.getId())).isEqualTo(48);
        assertThat(hoursBefore(reception.getId())).isEqualTo(48);
    }

    @Test
    void cancelMoreThan48HoursBeforeCheckIn() {
        var reservation = book(today.plusDays(5));

        reservationService.cancelByGuest(reservation.getReservationNumber(), "guest.online@example.com");

        var stored = dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(reservation.getId()));
        assertThat(stored.getStatus()).isEqualTo(ReservationStatus.CANCELLED.name());
        assertThat(stored.getCancelledAt()).isNotNull();
    }

    @Test
    void cancelWithin48HoursAsksToCallReception() {
        // Arrival tomorrow: the deadline (yesterday 15:00) has passed whatever the time of day
        for (var arrival : new LocalDate[]{today.plusDays(1), today}) {
            var reservation = book(arrival);
            var passedText = CancellationDeadline.of(arrival, 48).passedText();

            assertThat(reservationService.findForGuest(reservation.getReservationNumber(),
                    "guest.online@example.com")).hasValueSatisfying(g -> assertThat(g.cancellable()).isFalse());
            assertThatThrownBy(() -> reservationService.cancelByGuest(reservation.getReservationNumber(),
                    "guest.online@example.com"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage(passedText);
            dsl.deleteFrom(RESERVATION_NIGHT).where(RESERVATION_NIGHT.RESERVATION_ID.eq(reservation.getId())).execute();
            dsl.deleteFrom(RESERVATION).where(RESERVATION.ID.eq(reservation.getId())).execute();
        }
    }

    @Test
    void oldBookingCanStillBeCancelledTheDayBeforeArrival() {
        // A reservation from before 002 (V4 gave it 15 h); dates in hotel time so the test does not depend on
        // the machine's time zone
        var tomorrowInHotel = LocalDate.now(clock.withZone(CancellationDeadline.ZONE)).plusDays(1);
        var reservation = oldBooking(tomorrowInHotel);

        assertThat(reservationService.findForGuest(reservation.getReservationNumber(), "guest.online@example.com"))
                .hasValueSatisfying(g -> assertThat(g.cancellable()).isTrue());
        reservationService.cancelByGuest(reservation.getReservationNumber(), "guest.online@example.com");

        assertThat(dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(reservation.getId())).getStatus())
                .isEqualTo(ReservationStatus.CANCELLED.name());
    }

    @Test
    void oldBookingCannotBeCancelledOnTheArrivalDate() {
        var todayInHotel = LocalDate.now(clock.withZone(CancellationDeadline.ZONE));
        var reservation = oldBooking(todayInHotel);

        assertThatThrownBy(() -> reservationService.cancelByGuest(reservation.getReservationNumber(),
                "guest.online@example.com"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(CancellationDeadline.of(todayInHotel, 15).passedText())
                .hasMessageContaining(", 00:00 (Zurich time)");
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

    /**
     * Books well ahead, then moves the row to the given arrival with the old 15-hour rule.
     */
    private ReservationRecord oldBooking(LocalDate arrival) {
        var reservation = book(today.plusDays(30));
        dsl.update(RESERVATION)
                .set(RESERVATION.ARRIVAL_DATE, arrival)
                .set(RESERVATION.DEPARTURE_DATE, arrival.plusDays(2))
                .set(RESERVATION.CANCELLATION_HOURS_BEFORE, 15)
                .where(RESERVATION.ID.eq(reservation.getId())).execute();
        return reservation;
    }

    private int hoursBefore(long reservationId) {
        return dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(reservationId)).getCancellationHoursBefore();
    }

    private ReservationRecord book(LocalDate arrival) {
        return reservationService.book(new BookingRequest(roomId, arrival, arrival.plusDays(2), 2,
                "Test Online Guest", "guest.online@example.com", "+41 79 000 00 00", BookingSource.ONLINE));
    }

    @Test
    void lookupReturnsTheNightsInDateOrderAndTheirSum() {
        seasonalRateService.save(rate(typeId, "Test Summer", today.plusDays(10).toString(),
                today.plusDays(20).toString(), "150.00"));
        var reservation = reservationService.book(new BookingRequest(roomId, today.plusDays(9), today.plusDays(12), 2,
                "Test Online Guest", "guest.online@example.com", "+41 79 000 00 00", BookingSource.ONLINE));

        var found = reservationService.findForGuest(reservation.getReservationNumber(), "guest.online@example.com");

        assertThat(found).hasValueSatisfying(g -> {
            assertThat(g.nights()).extracting(NightRate::night)
                    .containsExactly(today.plusDays(9), today.plusDays(10), today.plusDays(11));
            assertThat(g.nights()).extracting(NightRate::rate).usingElementComparator(BigDecimal::compareTo)
                    .containsExactly(new BigDecimal("100"), new BigDecimal("150"), new BigDecimal("150"));
            assertThat(g.totalPrice()).isEqualByComparingTo(NightRate.total(g.nights()))
                    .isEqualByComparingTo("400.00");
        });
    }
}
