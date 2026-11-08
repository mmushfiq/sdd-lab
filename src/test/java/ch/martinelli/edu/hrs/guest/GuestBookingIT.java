package ch.martinelli.edu.hrs.guest;

import ch.martinelli.edu.hrs.core.ui.PlaywrightIT;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.room.RoomService;
import ch.martinelli.edu.hrs.room.RoomTypeService;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION_NIGHT;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class GuestBookingIT extends PlaywrightIT {

    private static final DateTimeFormatter US_DATE = DateTimeFormatter.ofPattern("M/d/yyyy");

    @Autowired
    private DSLContext dsl;

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private Clock clock;

    private long roomId;

    @BeforeEach
    void createData() {
        var type = roomTypeService.save(roomType("IT Online Double", 2, "120.00"));
        var room = new RoomRecord();
        room.setRoomNumber("IT-301");
        room.setRoomTypeId(type.getId());
        roomId = roomService.save(room).getId();
    }

    @AfterEach
    void cleanUp() {
        dsl.deleteFrom(RESERVATION_NIGHT).where(RESERVATION_NIGHT.RESERVATION_ID.in(
                dsl.select(RESERVATION.ID).from(RESERVATION).where(RESERVATION.ROOM_ID.eq(roomId)))).execute();
        dsl.deleteFrom(RESERVATION).where(RESERVATION.ROOM_ID.eq(roomId)).execute();
        dsl.deleteFrom(ROOM).where(ROOM.ID.eq(roomId)).execute();
        dsl.deleteFrom(ROOM_TYPE).where(ROOM_TYPE.NAME.eq("IT Online Double")).execute();
    }

    @Test
    void guestBooksOnlineWithoutLogin() {
        var arrival = LocalDate.now(clock).plusDays(10);
        page.navigate(url(""));
        mopo.waitForConnectionToSettle();

        fillDate("Arrival", arrival);
        fillDate("Departure", arrival.plusDays(2));
        page.getByLabel("Guests").fill("2");
        button("Search").click();
        mopo.waitForConnectionToSettle();
        page.getByText("IT-301").click();
        button("Book").click();
        page.getByLabel("Guest name").fill("IT Online Guest");
        page.getByLabel("Email").fill("it.online@example.com");
        page.getByLabel("Phone").fill("+41 79 222 22 22");
        button("Confirm booking").click();
        assertThat(page.getByText("Your reservation number is")).isVisible();

        var number = dsl.select(RESERVATION.RESERVATION_NUMBER).from(RESERVATION)
                .where(RESERVATION.ROOM_ID.eq(roomId)).fetchSingle().value1();
        assertThat(page.getByText(number)).isVisible();
    }

    @Test
    void staffPageWithoutLoginLeadsToLogin() {
        page.navigate(url("reception/reservations"));
        mopo.waitForConnectionToSettle();

        assertThat(page).hasURL(Pattern.compile(".*/login$"));
    }

    private void fillDate(String label, LocalDate date) {
        var field = page.getByLabel(label);
        field.fill(US_DATE.format(date));
        field.press("Enter");
    }

    private Locator button(String name) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
    }

    private String url(String route) {
        return "http://localhost:" + localServerPort + "/" + route;
    }
}
