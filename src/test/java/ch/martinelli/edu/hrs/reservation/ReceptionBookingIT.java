package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.core.security.Role;
import ch.martinelli.edu.hrs.core.ui.PlaywrightIT;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.room.RoomService;
import ch.martinelli.edu.hrs.room.RoomTypeService;
import ch.martinelli.edu.hrs.staff.StaffUserService;
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
import java.time.format.DateTimeFormatter;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION_NIGHT;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static ch.martinelli.edu.hrs.db.Tables.STAFF_USER;
import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class ReceptionBookingIT extends PlaywrightIT {

    private static final DateTimeFormatter US_DATE = DateTimeFormatter.ofPattern("M/d/yyyy");

    @Autowired
    private DSLContext dsl;

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private StaffUserService staffUserService;

    @Autowired
    private Clock clock;

    private long roomId;

    @BeforeEach
    void createData() {
        var type = roomTypeService.save(roomType("IT Double", 2, "120.00"));
        var room = new RoomRecord();
        room.setRoomNumber("IT-201");
        room.setRoomTypeId(type.getId());
        roomId = roomService.save(room).getId();
        staffUserService.create("it-rita", Role.RECEPTIONIST, "secret123");
    }

    @AfterEach
    void cleanUp() {
        dsl.deleteFrom(RESERVATION_NIGHT).where(RESERVATION_NIGHT.RESERVATION_ID.in(
                dsl.select(RESERVATION.ID).from(RESERVATION).where(RESERVATION.ROOM_ID.eq(roomId)))).execute();
        dsl.deleteFrom(RESERVATION).where(RESERVATION.ROOM_ID.eq(roomId)).execute();
        dsl.deleteFrom(ROOM).where(ROOM.ID.eq(roomId)).execute();
        dsl.deleteFrom(ROOM_TYPE).where(ROOM_TYPE.NAME.eq("IT Double")).execute();
        dsl.deleteFrom(STAFF_USER).where(STAFF_USER.USERNAME.eq("it-rita")).execute();
    }

    @Test
    void receptionistBooksRoomAndItIsNoLongerOffered() {
        var arrival = LocalDate.now(clock).plusDays(7);
        logIn("it-rita", "secret123");
        page.navigate(url("reception/reservations"));
        mopo.waitForConnectionToSettle();

        search(arrival, arrival.plusDays(2));
        page.getByText("IT-201").click();
        button("Book").click();
        page.getByLabel("Guest name", new Page.GetByLabelOptions().setExact(true)).fill("IT Guest");
        page.getByLabel("Email").fill("it.guest@example.com");
        page.getByLabel("Phone").fill("+41 79 111 11 11");
        button("Confirm booking").click();
        assertThat(page.getByText("booked.")).isVisible();

        // IT-201 is the only room, so a repeated search finds none (the grid keeps old text in hidden rows)
        search(arrival, arrival.plusDays(2));
        assertThat(page.getByText("No room is available for these dates and number of guests.")).isVisible();
    }

    private void search(LocalDate arrival, LocalDate departure) {
        fillDate("Arrival", arrival);
        fillDate("Departure", departure);
        page.getByLabel("Guests").fill("2");
        button("Search").click();
        mopo.waitForConnectionToSettle();
    }

    private void fillDate(String label, LocalDate date) {
        var field = page.getByLabel(label);
        field.fill(US_DATE.format(date));
        field.press("Enter");
    }

    private com.microsoft.playwright.Locator button(String name) {
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
