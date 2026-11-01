package ch.martinelli.edu.hrs.housekeeping.ui;

import ch.martinelli.edu.hrs.core.ui.PlaywrightIT;
import ch.martinelli.edu.hrs.housekeeping.HousekeepingService;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

class CleaningListIT extends PlaywrightIT {

    @Autowired
    DSLContext ctx;

    @AfterEach
    void makeAllRoomsClean() {
        ctx.update(ROOM).set(ROOM.CLEANING_STATUS, HousekeepingService.CLEAN).execute();
    }

    private String url(String path) {
        return "http://localhost:" + localServerPort + "/" + path;
    }

    // Scenarios "Tablet width" and "Room marked as cleaned"
    @Test
    void cleaning_list_on_a_tablet() {
        ctx.update(ROOM).set(ROOM.CLEANING_STATUS, HousekeepingService.DIRTY)
                .where(ROOM.ROOM_NUMBER.eq("104")).execute();
        page.setViewportSize(768, 1024);

        page.navigate(url("login"));
        page.getByLabel("Username").fill("housekeeping");
        page.getByLabel("Password", new Page.GetByLabelOptions().setExact(true)).fill("housekeeping");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log in")).click();
        page.waitForURL(url("staff"));
        // Below 800 px the navigation drawer is an overlay that starts closed
        page.locator("vaadin-drawer-toggle").click();
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Cleaning")).click();
        page.waitForURL(url("cleaning"));

        Locator room = page.locator("#room-104");
        Locator cleaned = page.locator("#cleaned-104");
        assertThat(room).isVisible();
        assertThat(cleaned).isVisible();
        assertThat((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= window.innerWidth"))
                .isTrue();
        BoundingBox box = cleaned.boundingBox();
        assertThat(box.height).isGreaterThanOrEqualTo(44);
        assertThat(box.width).isGreaterThanOrEqualTo(44);
        assertThat(box.x + box.width).isLessThanOrEqualTo(768);

        cleaned.click();
        assertThat(room).hasCount(0);
        assertThat(page.getByText("All rooms are clean")).isVisible();
    }
}
