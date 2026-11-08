package ch.martinelli.edu.hrs.frontdesk;

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

class FrontDeskIT extends PlaywrightIT {

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
        var type = roomTypeService.save(roomType("IT Front Desk", 2, "100.00"));
        var room = new RoomRecord();
        room.setRoomNumber("IT-401");
        room.setRoomTypeId(type.getId());
        roomId = roomService.save(room).getId();
        var today = LocalDate.now(clock);
        reservationService.book(new BookingRequest(roomId, today, today.plusDays(2), 2, "IT Stay Guest",
                "it.stay@example.com", "+41 79 333 33 33", BookingSource.RECEPTION));
        staffUserService.create("it-fred", Role.RECEPTIONIST, "secret123");
    }

    @AfterEach
    void cleanUp() {
        dsl.deleteFrom(RESERVATION).where(RESERVATION.ROOM_ID.eq(roomId)).execute();
        dsl.deleteFrom(ROOM).where(ROOM.ID.eq(roomId)).execute();
        dsl.deleteFrom(ROOM_TYPE).where(ROOM_TYPE.NAME.eq("IT Front Desk")).execute();
        dsl.deleteFrom(STAFF_USER).where(STAFF_USER.USERNAME.eq("it-fred")).execute();
    }

    @Test
    void receptionistChecksGuestInAndOut() {
        logIn("it-fred", "secret123");
        assertThat(page).hasURL(url("reception/front-desk"));

        page.locator("#arrivals").getByText("IT Stay Guest").click();
        button("Check in").click();
        assertThat(page.getByText("IT Stay Guest checked in to room IT-401.")).isVisible();

        // Let the stay depart today, then check out
        var today = LocalDate.now(clock);
        dsl.update(RESERVATION).set(RESERVATION.ARRIVAL_DATE, today.minusDays(1))
                .set(RESERVATION.DEPARTURE_DATE, today).where(RESERVATION.ROOM_ID.eq(roomId)).execute();
        page.reload();
        mopo.waitForConnectionToSettle();
        page.locator("#departures").getByText("IT Stay Guest").click();
        button("Check out").click();
        assertThat(page.getByText("IT Stay Guest checked out of room IT-401.")).isVisible();

        assertThat(page.locator("#overview").getByText("Dirty")).isVisible();
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
