package ch.martinelli.edu.hrs.room.ui;

import ch.martinelli.edu.hrs.core.ui.PlaywrightIT;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class RoomManagementIT extends PlaywrightIT {

    @Autowired
    DSLContext ctx;

    @AfterEach
    void restoreDoubleRate() {
        ctx.update(ROOM_TYPE).set(ROOM_TYPE.NIGHTLY_RATE, new BigDecimal("140.00"))
                .where(ROOM_TYPE.NAME.eq("Double")).execute();
    }

    private String url(String path) {
        return "http://localhost:" + localServerPort + "/" + path;
    }

    // Scenario "Nightly rate changed": rooms of the type are offered at the new rate
    @Test
    void changed_nightly_rate_shows_on_rooms() {
        page.navigate(url("login"));
        page.getByLabel("Username").fill("manager");
        page.getByLabel("Password", new Page.GetByLabelOptions().setExact(true)).fill("manager");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log in")).click();
        page.waitForURL(url("staff"));

        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Room types")).click();
        page.waitForURL(url("room-types"));
        page.getByText("Double", new Page.GetByTextOptions().setExact(true)).click();
        assertThat(page.getByLabel("Name")).hasValue("Double");
        page.getByLabel("Base rate").fill("150.00");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save")).click();
        assertThat(page.getByText("CHF 150.00")).isVisible();

        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Rooms").setExact(true)).click();
        page.waitForURL(url("rooms"));
        // Rooms 104-108 are the Double rooms.
        assertThat(page.getByText("104", new Page.GetByTextOptions().setExact(true))).isVisible();
        assertThat(page.getByText("CHF 150.00", new Page.GetByTextOptions().setExact(true))).hasCount(5);
        assertThat(page.getByText("CHF 140.00", new Page.GetByTextOptions().setExact(true))).hasCount(0);
    }
}
