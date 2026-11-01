package ch.martinelli.edu.hrs.frontdesk.ui;

import ch.martinelli.edu.hrs.core.ui.PlaywrightIT;
import ch.martinelli.edu.hrs.housekeeping.HousekeepingService;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.reservation.ReservationTestData.insert;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class CheckInOutIT extends PlaywrightIT {

    @Autowired
    DSLContext ctx;

    @AfterEach
    void cleanUp() {
        ctx.deleteFrom(RESERVATION).execute();
        ctx.update(ROOM).set(ROOM.CLEANING_STATUS, HousekeepingService.CLEAN).execute();
    }

    private String url(String path) {
        return "http://localhost:" + localServerPort + "/" + path;
    }

    // Scenarios "Successful check-in", "Successful check-out" and the room on the cleaning list
    @Test
    void check_in_and_out_room_104_hands_it_to_housekeeping() {
        LocalDate today = LocalDate.now();
        insert(ctx, "ANNA0001", "104", today, today.plusDays(2), "Anna Meier", "BOOKED");

        login("reception");
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Front desk")).click();
        page.waitForURL(url("front-desk"));
        assertThat(page.locator("#state-104")).hasText("Arrival today");

        page.locator("#check-in-104").click();
        confirm("Check in");
        assertThat(page.locator("#state-104")).hasText("Occupied");

        page.locator("#check-out-104").click();
        confirm("Check out");
        assertThat(page.locator("#state-104")).hasText("Free");
        assertThat(page.locator("#cleaning-104")).hasText("Dirty");

        page.locator("#layout-logout").click();
        page.waitForURL(url("login"));
        login("housekeeping");
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Cleaning")).click();
        page.waitForURL(url("cleaning"));
        assertThat(page.locator("#room-104")).isVisible();
    }

    private void login(String user) {
        page.navigate(url("login"));
        page.getByLabel("Username").fill(user);
        page.getByLabel("Password", new Page.GetByLabelOptions().setExact(true)).fill(user);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log in")).click();
        page.waitForURL(url("staff"));
    }

    private void confirm(String button) {
        page.locator("vaadin-confirm-dialog").getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(button)).click();
    }
}
