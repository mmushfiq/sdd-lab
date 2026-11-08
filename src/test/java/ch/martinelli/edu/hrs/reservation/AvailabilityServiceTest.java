package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
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
class AvailabilityServiceTest {

    @Autowired
    private AvailabilityService availabilityService;

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
    private long doubleRoomId;

    @BeforeEach
    void createRooms() {
        today = LocalDate.now(clock);
        var single = roomTypeService.save(roomType("Test Single", 1, "80.00"));
        var dbl = roomTypeService.save(roomType("Test Double", 2, "120.00"));
        room("T501", single.getId());
        doubleRoomId = room("T502", dbl.getId());
    }

    @Test
    void capacityFiltersRoomsAndTotalPriceIsShown() {
        var result = availabilityService.search(today.plusDays(1), today.plusDays(3), 2);

        assertThat(result).extracting(AvailableRoom::roomNumber).contains("T502").doesNotContain("T501");
        assertThat(result).filteredOn(r -> r.roomNumber().equals("T502")).singleElement()
                .satisfies(r -> assertThat(r.totalPrice()).isEqualByComparingTo("240.00"));
    }

    @Test
    void overlappingNightsAreExcluded() {
        book(doubleRoomId, today.plusDays(10), today.plusDays(12));

        assertThat(roomNumbers(today.plusDays(11), today.plusDays(13), 2)).doesNotContain("T502");
        assertThat(roomNumbers(today.plusDays(9), today.plusDays(11), 2)).doesNotContain("T502");
    }

    @Test
    void departureDayOfOneStayCanBeArrivalOfNext() {
        book(doubleRoomId, today.plusDays(10), today.plusDays(12));

        assertThat(roomNumbers(today.plusDays(12), today.plusDays(14), 2)).contains("T502");
        assertThat(roomNumbers(today.plusDays(8), today.plusDays(10), 2)).contains("T502");
    }

    @Test
    void checkedOutAndCancelledReservationsDoNotBlock() {
        var first = book(doubleRoomId, today.plusDays(20), today.plusDays(22));
        dsl.update(RESERVATION).set(RESERVATION.STATUS, ReservationStatus.CANCELLED.name())
                .where(RESERVATION.ID.eq(first)).execute();
        var second = book(doubleRoomId, today.plusDays(20), today.plusDays(22));
        dsl.update(RESERVATION).set(RESERVATION.STATUS, ReservationStatus.CHECKED_OUT.name())
                .where(RESERVATION.ID.eq(second)).execute();

        assertThat(roomNumbers(today.plusDays(20), today.plusDays(22), 2)).contains("T502");
    }

    @Test
    void checkedInReservationBlocks() {
        var id = book(doubleRoomId, today.plusDays(20), today.plusDays(22));
        dsl.update(RESERVATION).set(RESERVATION.STATUS, ReservationStatus.CHECKED_IN.name())
                .where(RESERVATION.ID.eq(id)).execute();

        assertThat(roomNumbers(today.plusDays(21), today.plusDays(23), 2)).doesNotContain("T502");
    }

    @Test
    void dateRulesAreEnforced() {
        assertThatThrownBy(() -> availabilityService.search(today.plusDays(2), today.plusDays(2), 1))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Departure must be after arrival.");
        assertThatThrownBy(() -> availabilityService.search(today.minusDays(1), today.plusDays(1), 1))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Arrival cannot be in the past.");
        assertThatThrownBy(() -> availabilityService.search(today.plusYears(1).plusDays(1), today.plusYears(1).plusDays(2), 1))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Arrival can be at most one year ahead.");
        assertThatThrownBy(() -> availabilityService.search(today.plusDays(1), today.plusDays(32), 1))
                .isInstanceOf(BusinessRuleException.class).hasMessage("A stay can be at most 30 nights.");
        assertThat(availabilityService.search(today, today.plusDays(30), 1)).isNotNull();
    }

    @Test
    void noRoomFitsTooManyGuests() {
        assertThat(availabilityService.search(today.plusDays(1), today.plusDays(2), 50)).isEmpty();
    }

    private java.util.List<String> roomNumbers(LocalDate arrival, LocalDate departure, int guests) {
        return availabilityService.search(arrival, departure, guests).stream().map(AvailableRoom::roomNumber).toList();
    }

    private long book(long roomId, LocalDate arrival, LocalDate departure) {
        return reservationService.book(new BookingRequest(roomId, arrival, departure, 2,
                "Test Guest", "guest@example.com", "+41 79 000 00 00", BookingSource.RECEPTION)).getId();
    }

    private long room(String number, long typeId) {
        var room = new RoomRecord();
        room.setRoomNumber(number);
        room.setRoomTypeId(typeId);
        return roomService.save(room).getId();
    }
}
