package ch.martinelli.edu.hrs.housekeeping;

import ch.martinelli.edu.hrs.core.security.Role;
import ch.martinelli.edu.hrs.core.ui.PlaywrightIT;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.reservation.BookingRequest;
import ch.martinelli.edu.hrs.reservation.BookingSource;
import ch.martinelli.edu.hrs.reservation.ReservationService;
import ch.martinelli.edu.hrs.room.RoomService;
import ch.martinelli.edu.hrs.room.RoomTypeService;
import ch.martinelli.edu.hrs.staff.StaffUserService;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.LocalDate;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static ch.martinelli.edu.hrs.db.Tables.STAFF_USER;
import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class HousekeepingIT extends PlaywrightIT {

    @Autowired
    private DSLContext dsl;

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private StaffUserService staffUserService;

    @Autowired
    private Clock clock;

    private long roomId;

    @BeforeEach
    void createData() {
        var type = roomTypeService.save(roomType("IT Housekeeping", 2, "100.00"));
        var room = new RoomRecord();
        room.setRoomNumber("IT-501");
        room.setRoomTypeId(type.getId());
        roomId = roomService.save(room).getId();
        var today = LocalDate.now(clock);
        var reservation = reservationService.book(new BookingRequest(roomId, today, today.plusDays(1), 1,
                "IT Clean Guest", "it.clean@example.com", "+41 79 444 44 44", BookingSource.RECEPTION));
        reservationService.checkIn(reservation.getId());
        reservationService.checkOut(reservation.getId());
        staffUserService.create("it-hanna", Role.HOUSEKEEPING, "secret123");
    }

    @AfterEach
    void cleanUp() {
        dsl.deleteFrom(RESERVATION).where(RESERVATION.ROOM_ID.eq(roomId)).execute();
        dsl.deleteFrom(ROOM).where(ROOM.ID.eq(roomId)).execute();
        dsl.deleteFrom(ROOM_TYPE).where(ROOM_TYPE.NAME.eq("IT Housekeeping")).execute();
        dsl.deleteFrom(STAFF_USER).where(STAFF_USER.USERNAME.eq("it-hanna")).execute();
    }

    @Test
    void housekeepingMarksCheckedOutRoomCleanedOnTablet() {
        page.setViewportSize(1024, 768);
        logIn("it-hanna", "secret123");
        assertThat(page).hasURL(url("housekeeping"));

        page.getByText("IT-501").click();
        button("Mark cleaned").click();
        mopo.waitForConnectionToSettle();

        // The grid keeps old text in hidden rows, so check the stored status; no guest data is shown (FR-024)
        org.assertj.core.api.Assertions.assertThat(
                dsl.fetchSingle(ROOM, ROOM.ID.eq(roomId)).getCleaningStatus()).isEqualTo("CLEAN");
        assertThat(page.getByText("IT Clean Guest")).hasCount(0);
    }

    private Locator button(String name) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
    }

    private void logIn(String username, String password) {
        page.navigate(url("login"));
        mopo.waitForConnectionToSettle();
        page.locator("input[name='username']").fill(username);
        page.locator("input[name='password']").fill(password);
        button("Log in").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        mopo.waitForConnectionToSettle();
    }

    private String url(String route) {
        return "http://localhost:" + localServerPort + "/" + route;
    }
}
