package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
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
import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION_NIGHT;
import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static ch.martinelli.edu.hrs.room.SeasonalRateServiceTest.rate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ReservationServiceTest {

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
    private RoomTypeRecord type;
    private long roomId;

    @BeforeEach
    void createRoom() {
        today = LocalDate.now(clock);
        type = roomTypeService.save(roomType("Test Double", 2, "120.00"));
        var room = new RoomRecord();
        room.setRoomNumber("T601");
        room.setRoomTypeId(type.getId());
        roomId = roomService.save(room).getId();
    }

    @Test
    void bookingCreatesBookedReservation() {
        var reservation = reservationService.book(request(today.plusDays(1), today.plusDays(3)));

        var stored = dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(reservation.getId()));
        assertThat(stored.getReservationNumber()).matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}");
        assertThat(stored.getStatus()).isEqualTo(ReservationStatus.BOOKED.name());
        assertThat(stored.getSource()).isEqualTo(BookingSource.RECEPTION.name());
        assertNights(reservation.getId(), "120.00", "120.00");
        assertThat(stored.getTotalPrice()).isEqualByComparingTo("240.00");
        assertThat(stored.getGuestName()).isEqualTo("Test Guest");
        assertThat(stored.getGuestEmail()).isEqualTo("guest@example.com");
        assertThat(stored.getNumberOfGuests()).isEqualTo(2);
    }

    @Test
    void overlappingBookingIsRejected() {
        reservationService.book(request(today.plusDays(10), today.plusDays(12)));

        assertThatThrownBy(() -> reservationService.book(request(today.plusDays(11), today.plusDays(13))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("The room is no longer available for these dates.");
    }

    @Test
    void rateChangeKeepsExistingReservationRate() {
        var reservation = reservationService.book(request(today.plusDays(1), today.plusDays(3)));

        type.setNightlyRate(new BigDecimal("150.00"));
        roomTypeService.save(type);

        var stored = dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(reservation.getId()));
        assertNights(reservation.getId(), "120.00", "120.00");
        assertThat(stored.getTotalPrice()).isEqualByComparingTo("240.00");
    }

    @Test
    void bookingAcrossTheSeasonStartStoresTheRateOfEachNight() {
        seasonalRateService.save(rate(type.getId(), "Test Summer", today.plusDays(10).toString(),
                today.plusDays(20).toString(), "150.00"));

        var reservation = reservationService.book(request(today.plusDays(9), today.plusDays(12)));

        assertThat(dsl.selectFrom(RESERVATION_NIGHT).where(RESERVATION_NIGHT.RESERVATION_ID.eq(reservation.getId()))
                .orderBy(RESERVATION_NIGHT.NIGHT_DATE).fetch(RESERVATION_NIGHT.NIGHT_DATE))
                .containsExactly(today.plusDays(9), today.plusDays(10), today.plusDays(11));
        assertNights(reservation.getId(), "120.00", "150.00", "150.00");
        assertThat(dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(reservation.getId())).getTotalPrice())
                .isEqualByComparingTo("420.00");
        assertThat(reservationService.nights(reservation.getId())).extracting(NightRate::night)
                .containsExactly(today.plusDays(9), today.plusDays(10), today.plusDays(11));
    }

    @Test
    void tooManyGuestsForRoomIsRejected() {
        var request = new BookingRequest(roomId, today.plusDays(1), today.plusDays(2), 3,
                "Test Guest", "guest@example.com", "+41 79 000 00 00", BookingSource.RECEPTION);

        assertThatThrownBy(() -> reservationService.book(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("The room is too small for this number of guests.");
    }

    @Test
    void guestDetailsAreRequired() {
        var request = new BookingRequest(roomId, today.plusDays(1), today.plusDays(2), 2,
                " ", "guest@example.com", "+41 79 000 00 00", BookingSource.RECEPTION);

        assertThatThrownBy(() -> reservationService.book(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Guest name is required.");
    }

    @Test
    void findByReservationNumberOrGuestName() {
        var reservation = reservationService.book(new BookingRequest(roomId, today.plusDays(1), today.plusDays(2),
                1, "Test Findable Guest", "find@example.com", "+41 79 000 00 00", BookingSource.RECEPTION));

        assertThat(reservationService.find(reservation.getReservationNumber().toLowerCase()))
                .extracting(f -> f.reservation().getId()).containsExactly(reservation.getId());
        assertThat(reservationService.find("findable"))
                .singleElement().satisfies(f -> {
                    assertThat(f.roomNumber()).isEqualTo("T601");
                    assertThat(f.nights()).extracting(NightRate::night).containsExactly(today.plusDays(1));
                    assertThat(f.nights()).extracting(NightRate::rate).usingElementComparator(BigDecimal::compareTo)
                            .containsExactly(new BigDecimal("120.00"));
                });
        assertThat(reservationService.find(" ")).isEmpty();
    }

    @Test
    void changeToFreeNightsKeepsSavedNightsAndPricesNewOnesWithTheCurrentRate() {
        var reservation = reservationService.book(request(today.plusDays(1), today.plusDays(3)));
        type.setNightlyRate(new BigDecimal("150.00"));
        roomTypeService.save(type);

        reservationService.change(reservation.getId(), new BookingRequest(roomId, today.plusDays(2),
                today.plusDays(5), 1, "Test Changed", "changed@example.com", "+41 79 000 00 09", null));

        var stored = dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(reservation.getId()));
        assertThat(stored.getArrivalDate()).isEqualTo(today.plusDays(2));
        assertThat(stored.getDepartureDate()).isEqualTo(today.plusDays(5));
        assertThat(stored.getNumberOfGuests()).isEqualTo(1);
        assertThat(stored.getGuestName()).isEqualTo("Test Changed");
        // 003 FR-007: the night it already had keeps 120, the new nights get today's 150
        assertNights(reservation.getId(), "120.00", "150.00", "150.00");
        assertThat(stored.getTotalPrice()).isEqualByComparingTo("420.00");
        assertThat(stored.getReservationNumber()).isEqualTo(reservation.getReservationNumber());
        assertThat(stored.getSource()).isEqualTo(BookingSource.RECEPTION.name());
    }

    @Test
    void changeOfGuestDetailsKeepsBookedRate() {
        var reservation = reservationService.book(request(today.plusDays(1), today.plusDays(3)));
        type.setNightlyRate(new BigDecimal("150.00"));
        roomTypeService.save(type);

        reservationService.change(reservation.getId(), new BookingRequest(roomId, today.plusDays(1),
                today.plusDays(3), 1, "Test Guest", "guest@example.com", "+41 79 999 99 99", null));

        var stored = dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(reservation.getId()));
        assertThat(stored.getGuestPhone()).isEqualTo("+41 79 999 99 99");
        assertThat(stored.getNumberOfGuests()).isEqualTo(1);
        assertNights(reservation.getId(), "120.00", "120.00");
        assertThat(stored.getTotalPrice()).isEqualByComparingTo("240.00");
    }

    @Test
    void changeToAnotherFreeRoom() {
        var reservation = reservationService.book(request(today.plusDays(1), today.plusDays(3)));
        var other = new RoomRecord();
        other.setRoomNumber("T602");
        other.setRoomTypeId(type.getId());
        var otherId = roomService.save(other).getId();

        reservationService.change(reservation.getId(), new BookingRequest(otherId, today.plusDays(1),
                today.plusDays(3), 2, "Test Guest", "guest@example.com", "+41 79 000 00 00", null));

        assertThat(dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(reservation.getId())).getRoomId())
                .isEqualTo(otherId);
    }

    @Test
    void changeIntoBookedNightsIsRejected() {
        reservationService.book(request(today.plusDays(10), today.plusDays(12)));
        var reservation = reservationService.book(request(today.plusDays(1), today.plusDays(3)));

        assertThatThrownBy(() -> reservationService.change(reservation.getId(),
                request(today.plusDays(2), today.plusDays(11))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("The room is no longer available for these dates.");
    }

    @Test
    void cancelFreesTheNights() {
        var reservation = reservationService.book(request(today.plusDays(1), today.plusDays(3)));

        reservationService.cancel(reservation.getId());

        var stored = dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(reservation.getId()));
        assertThat(stored.getStatus()).isEqualTo(ReservationStatus.CANCELLED.name());
        assertThat(stored.getCancelledAt()).isNotNull();
        reservationService.book(request(today.plusDays(1), today.plusDays(3)));
    }

    @Test
    void onlyBookedReservationsCanBeChangedOrCancelled() {
        var cancelled = reservationService.book(request(today.plusDays(1), today.plusDays(3)));
        reservationService.cancel(cancelled.getId());
        var checkedIn = reservationService.book(request(today, today.plusDays(1)));
        reservationService.checkIn(checkedIn.getId());

        for (var id : new long[]{cancelled.getId(), checkedIn.getId()}) {
            assertThatThrownBy(() -> reservationService.change(id, request(today.plusDays(5), today.plusDays(6))))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Only a booked reservation can be changed.");
            assertThatThrownBy(() -> reservationService.cancel(id))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Only a booked reservation can be cancelled.");
        }
    }

    @Test
    void dateChangeKeepsTheCancellationRuleAndMovesTheDeadline() {
        var newRule = reservationService.book(request(today.plusDays(10), today.plusDays(12)));
        var oldRule = reservationService.book(request(today.plusDays(20), today.plusDays(22)));
        dsl.update(RESERVATION).set(RESERVATION.CANCELLATION_HOURS_BEFORE, 15)
                .where(RESERVATION.ID.eq(oldRule.getId())).execute();

        reservationService.change(newRule.getId(), request(today.plusDays(17), today.plusDays(19)));
        reservationService.change(oldRule.getId(), request(today.plusDays(27), today.plusDays(29)));

        var changedNew = dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(newRule.getId()));
        var changedOld = dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(oldRule.getId()));
        assertThat(changedNew.getCancellationHoursBefore()).isEqualTo(48);
        assertThat(changedOld.getCancellationHoursBefore()).isEqualTo(15);
        assertThat(CancellationDeadline.of(changedNew.getArrivalDate(), 48).deadline())
                .isEqualTo(CancellationDeadline.of(today.plusDays(10), 48).deadline().plusDays(7));
    }

    @Test
    void receptionCanCancelAfterTheOnlineDeadline() {
        // Arrival tomorrow, rule 48: the online deadline has passed (FR-007)
        var reservation = reservationService.book(request(today.plusDays(1), today.plusDays(3)));

        reservationService.cancel(reservation.getId());

        assertThat(dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(reservation.getId())).getStatus())
                .isEqualTo(ReservationStatus.CANCELLED.name());
    }

    @Test
    void rateMaintenanceNeverChangesSavedNights() {
        var reservation = reservationService.book(request(today.plusDays(10), today.plusDays(13)));

        var summer = seasonalRateService.save(rate(type.getId(), "Test Summer", today.plusDays(5).toString(),
                today.plusDays(20).toString(), "150.00"));
        summer.setNightlyRate(new BigDecimal("170.00"));
        seasonalRateService.save(summer);
        type.setNightlyRate(new BigDecimal("90.00"));
        roomTypeService.save(type);
        seasonalRateService.delete(summer.getId());

        assertNights(reservation.getId(), "120.00", "120.00", "120.00");
        assertThat(total(reservation.getId())).isEqualByComparingTo("360.00");
    }

    @Test
    void extendingKeepsTheSavedNightsAndPricesTheNewNightWithTodaysRate() {
        var reservation = reservationService.book(request(today.plusDays(10), today.plusDays(12)));
        seasonalRateService.save(rate(type.getId(), "Test Summer", today.plusDays(10).toString(),
                today.plusDays(20).toString(), "150.00"));

        reservationService.change(reservation.getId(), request(today.plusDays(10), today.plusDays(13)));

        assertNights(reservation.getId(), "120.00", "120.00", "150.00");
        assertThat(total(reservation.getId())).isEqualByComparingTo("390.00");
    }

    @Test
    void moveToAnotherRoomTypePricesEveryNightWithThatTypesCurrentRates() {
        var reservation = reservationService.book(request(today.plusDays(10), today.plusDays(13)));
        var suite = roomTypeService.save(roomType("Test Suite", 2, "200.00"));
        seasonalRateService.save(rate(suite.getId(), "Test Suite Summer", today.plusDays(11).toString(),
                today.plusDays(20).toString(), "250.00"));
        var suiteRoom = new RoomRecord();
        suiteRoom.setRoomNumber("T603");
        suiteRoom.setRoomTypeId(suite.getId());
        var suiteRoomId = roomService.save(suiteRoom).getId();

        reservationService.change(reservation.getId(), new BookingRequest(suiteRoomId, today.plusDays(10),
                today.plusDays(13), 2, "Test Guest", "guest@example.com", "+41 79 000 00 00", null));

        assertNights(reservation.getId(), "200.00", "250.00", "250.00");
        assertThat(total(reservation.getId())).isEqualByComparingTo("700.00");
    }

    @Test
    void moveToAnotherRoomOfTheSameTypeKeepsThePrice() {
        var reservation = reservationService.book(request(today.plusDays(10), today.plusDays(12)));
        var other = new RoomRecord();
        other.setRoomNumber("T604");
        other.setRoomTypeId(type.getId());
        var otherId = roomService.save(other).getId();
        type.setNightlyRate(new BigDecimal("150.00"));
        roomTypeService.save(type);

        reservationService.change(reservation.getId(), new BookingRequest(otherId, today.plusDays(10),
                today.plusDays(12), 2, "Test Guest", "guest@example.com", "+41 79 000 00 00", null));

        assertNights(reservation.getId(), "120.00", "120.00");
        assertThat(total(reservation.getId())).isEqualByComparingTo("240.00");
    }

    @Test
    void shorteningDropsTheRemovedNightAndKeepsTheOthers() {
        var reservation = reservationService.book(request(today.plusDays(10), today.plusDays(13)));
        type.setNightlyRate(new BigDecimal("150.00"));
        roomTypeService.save(type);

        reservationService.change(reservation.getId(), request(today.plusDays(10), today.plusDays(12)));

        assertThat(reservationService.nights(reservation.getId())).extracting(NightRate::night)
                .containsExactly(today.plusDays(10), today.plusDays(11));
        assertNights(reservation.getId(), "120.00", "120.00");
        assertThat(total(reservation.getId())).isEqualByComparingTo("240.00");
    }

    private BigDecimal total(long reservationId) {
        return dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(reservationId)).getTotalPrice();
    }

    private BookingRequest request(LocalDate arrival, LocalDate departure) {
        return new BookingRequest(roomId, arrival, departure, 2,
                "Test Guest", "guest@example.com", "+41 79 000 00 00", BookingSource.RECEPTION);
    }

    private void assertNights(long reservationId, String... rates) {
        List<BigDecimal> stored = dsl.select(RESERVATION_NIGHT.NIGHTLY_RATE).from(RESERVATION_NIGHT)
                .where(RESERVATION_NIGHT.RESERVATION_ID.eq(reservationId))
                .orderBy(RESERVATION_NIGHT.NIGHT_DATE)
                .fetch(RESERVATION_NIGHT.NIGHTLY_RATE);
        assertThat(stored).usingElementComparator(BigDecimal::compareTo)
                .containsExactly(java.util.Arrays.stream(rates).map(BigDecimal::new).toArray(BigDecimal[]::new));
    }
}
