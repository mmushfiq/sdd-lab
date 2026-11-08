package ch.martinelli.edu.hrs.room;

import ch.martinelli.edu.hrs.core.ui.PlaywrightIT;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
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
import java.time.format.DateTimeFormatter;

import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static ch.martinelli.edu.hrs.db.Tables.SEASONAL_RATE;
import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * 003 US1, US2: the Manager maintains seasonal rates in the browser; guest prices use them.
 */
class SeasonalRatesIT extends PlaywrightIT {

    private static final DateTimeFormatter US_DATE = DateTimeFormatter.ofPattern("M/d/yyyy");

    @Autowired
    private DSLContext dsl;

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private Clock clock;

    @BeforeEach
    void createData() {
        var type = roomTypeService.save(roomType("IT Season Double", 2, "100.00"));
        var room = new RoomRecord();
        room.setRoomNumber("IT-S01");
        room.setRoomTypeId(type.getId());
        roomService.save(room);
    }

    @AfterEach
    void cleanUp() {
        dsl.deleteFrom(ROOM).where(ROOM.ROOM_NUMBER.eq("IT-S01")).execute();
        // on delete cascade removes the seasonal rates
        dsl.deleteFrom(ROOM_TYPE).where(ROOM_TYPE.NAME.eq("IT Season Double")).execute();
    }

    @Test
    void managerAddsAndDeletesASeasonalRate() {
        logIn("manager", "manager");
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Seasonal rates")).click();
        assertThat(page).hasURL(url("manager/seasonal-rates"));
        mopo.waitForConnectionToSettle();
        addSeasonalRate(LocalDate.now(clock).plusDays(10));

        page.getByText("Test Summer").click();
        button("Delete").click();
        mopo.waitForConnectionToSettle();
        // Vaadin keeps the emptied grid cell in the DOM, so check visibility and the database
        assertThat(page.getByText("Test Summer")).isHidden();
        org.assertj.core.api.Assertions.assertThat(dsl.fetchCount(SEASONAL_RATE, SEASONAL_RATE.NAME.eq("Test Summer")))
                .isZero();
    }

    /**
     * 003 US2/AC1: a Guest search inside the season shows the total at the seasonal rate.
     */
    @Test
    void guestSearchUsesTheSeasonalRate() {
        var start = LocalDate.now(clock).plusDays(10);
        logIn("manager", "manager");
        page.navigate(url("manager/seasonal-rates"));
        mopo.waitForConnectionToSettle();
        addSeasonalRate(start);
        button("Log out").click();
        mopo.waitForConnectionToSettle();

        page.navigate(url(""));
        mopo.waitForConnectionToSettle();
        fillDate("Arrival", start.plusDays(2));
        fillDate("Departure", start.plusDays(4));
        page.getByLabel("Guests").fill("2");
        button("Search").click();
        mopo.waitForConnectionToSettle();

        assertThat(page.getByText("IT-S01")).isVisible();
        assertThat(page.getByText("300.00")).isVisible();
    }

    private void addSeasonalRate(LocalDate start) {
        page.getByLabel("Room type").fill("IT Season Double");
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("IT Season Double")).click();
        page.getByLabel("Name").fill("Test Summer");
        fillDate("Start date", start);
        fillDate("End date", start.plusDays(10));
        page.getByLabel("Nightly rate").fill("150.00");
        button("Save").click();
        mopo.waitForConnectionToSettle();
        assertThat(page.getByText("Test Summer")).isVisible();
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
