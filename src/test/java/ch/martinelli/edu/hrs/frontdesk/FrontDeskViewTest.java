package ch.martinelli.edu.hrs.frontdesk;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import ch.martinelli.edu.hrs.reservation.BookingRequest;
import ch.martinelli.edu.hrs.reservation.BookingSource;
import ch.martinelli.edu.hrs.reservation.Occupancy;
import ch.martinelli.edu.hrs.reservation.ReservationService;
import ch.martinelli.edu.hrs.reservation.RoomStatus;
import ch.martinelli.edu.hrs.reservation.Stay;
import ch.martinelli.edu.hrs.room.RoomService;
import ch.martinelli.edu.hrs.room.RoomTypeService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@WithMockUser(roles = "RECEPTIONIST")
class FrontDeskViewTest extends HrsBrowserlessTest {

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
    private RoomTypeRecord type;

    @BeforeEach
    void createType() {
        today = LocalDate.now(clock);
        type = roomTypeService.save(roomType("Test Front Desk", 2, "100.00"));
    }

    @Test
    void checkInAndOutFromTodaysLists() {
        var roomId = room("T721");
        book(roomId, "Test Arriving");
        navigate(FrontDeskView.class);

        test(stays("arrivals")).select(row(stays("arrivals"), s -> s.guestName().equals("Test Arriving")));
        test($(Button.class).withText("Check in").single()).click();

        assertThat(test($(Notification.class).last()).getText())
                .isEqualTo("Test Arriving checked in to room T721.");
        assertThat(row(stays("arrivals"), s -> s.guestName().equals("Test Arriving"))).isNegative();
        assertThat(overviewRow("T721").occupancy()).isEqualTo(Occupancy.OCCUPIED);

        // The stay departs today once its dates are moved; check-out marks the room dirty
        dsl.update(RESERVATION).set(RESERVATION.ARRIVAL_DATE, today.minusDays(1))
                .set(RESERVATION.DEPARTURE_DATE, today).where(RESERVATION.ROOM_ID.eq(roomId)).execute();
        navigate(FrontDeskView.class);
        test(stays("departures")).select(row(stays("departures"), s -> s.guestName().equals("Test Arriving")));
        test($(Button.class).withText("Check out").single()).click();

        assertThat(test($(Notification.class).last()).getText())
                .isEqualTo("Test Arriving checked out of room T721.");
        var room = overviewRow("T721");
        assertThat(room.occupancy()).isEqualTo(Occupancy.FREE);
        assertThat(room.cleaningStatus()).isEqualTo("DIRTY");
    }

    @Test
    void checkInToDirtyRoomShowsError() {
        var roomId = room("T722");
        book(roomId, "Test Dirty");
        dsl.update(ROOM).set(ROOM.CLEANING_STATUS, "DIRTY").where(ROOM.ID.eq(roomId)).execute();
        navigate(FrontDeskView.class);

        test(stays("arrivals")).select(row(stays("arrivals"), s -> s.guestName().equals("Test Dirty")));
        test($(Button.class).withText("Check in").single()).click();

        assertThat(test($(Notification.class).last()).getText()).isEqualTo("The room is not clean yet.");
    }

    @Test
    void overviewShowsEveryRoom() {
        room("T723");
        navigate(FrontDeskView.class);

        var room = overviewRow("T723");
        assertThat(room.occupancy()).isEqualTo(Occupancy.FREE);
        assertThat(room.cleaningStatus()).isEqualTo("CLEAN");
        assertThat(test(overview()).getCellText(rowOfRoom("T723"), 2)).isEqualTo("Free");
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerIsDenied() {
        assertThat(opens(FrontDeskView.class)).isFalse();
    }

    @Test
    @WithMockUser(roles = "HOUSEKEEPING")
    void housekeepingIsDenied() {
        assertThat(opens(FrontDeskView.class)).isFalse();
    }

    private long room(String number) {
        var room = new RoomRecord();
        room.setRoomNumber(number);
        room.setRoomTypeId(type.getId());
        return roomService.save(room).getId();
    }

    private void book(long roomId, String guest) {
        reservationService.book(new BookingRequest(roomId, today, today.plusDays(2), 2, guest,
                "guest@example.com", "+41 79 000 00 00", BookingSource.RECEPTION));
    }

    @SuppressWarnings("unchecked")
    private Grid<Stay> stays(String id) {
        return $(Grid.class).id(id);
    }

    @SuppressWarnings("unchecked")
    private Grid<RoomStatus> overview() {
        return $(Grid.class).id("overview");
    }

    private RoomStatus overviewRow(String roomNumber) {
        return test(overview()).getRow(rowOfRoom(roomNumber));
    }

    private int rowOfRoom(String roomNumber) {
        var grid = test(overview());
        return IntStream.range(0, grid.size()).filter(i -> grid.getRow(i).roomNumber().equals(roomNumber))
                .findFirst().orElseThrow();
    }

    private int row(Grid<Stay> grid, Predicate<Stay> match) {
        var tester = test(grid);
        return IntStream.range(0, tester.size()).filter(i -> match.test(tester.getRow(i)))
                .findFirst().orElse(-1);
    }
}
