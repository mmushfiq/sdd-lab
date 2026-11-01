package ch.martinelli.edu.hrs.report.ui;

import ch.martinelli.edu.hrs.core.ui.PlaywrightIT;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION_NIGHT;
import static ch.martinelli.edu.hrs.reservation.ReservationTestData.insert;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class ReportIT extends PlaywrightIT {

    @Autowired
    DSLContext ctx;

    @AfterEach
    void cleanUp() {
        ctx.deleteFrom(RESERVATION_NIGHT).execute();
        ctx.deleteFrom(RESERVATION).execute();
    }

    private String url(String path) {
        return "http://localhost:" + localServerPort + "/" + path;
    }

    // Scenarios "Report for the current year", "Occupancy computed" and "Revenue from saved rates"
    @Test
    void manager_sees_april_figures_of_the_current_year() {
        int year = LocalDate.now().getYear();
        // April: 3 nights at 140.00 + 2 nights at 220.00 = 860.00; 5 / (10 rooms x 30 days) = 1.7 %
        insert(ctx, "REPORT01", "104", LocalDate.of(year, 4, 1), LocalDate.of(year, 4, 4), "Anna", "CHECKED_OUT");
        insert(ctx, "REPORT02", "109", LocalDate.of(year, 4, 10), LocalDate.of(year, 4, 12), "Ben", "CHECKED_OUT",
                "220.00", "220.00");

        page.navigate(url("login"));
        page.getByLabel("Username").fill("manager");
        page.getByLabel("Password", new Page.GetByLabelOptions().setExact(true)).fill("manager");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log in")).click();
        page.waitForURL(url("staff"));
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Reports")).click();
        page.waitForURL(url("reports"));

        Locator cells = page.locator("#report vaadin-grid-cell-content");
        assertThat(cells.getByText("April", new Locator.GetByTextOptions().setExact(true))).isVisible();
        assertThat(cells.getByText("1.7 %", new Locator.GetByTextOptions().setExact(true))).isVisible();
        // The April row and the year total
        assertThat(cells.getByText("CHF 860.00", new Locator.GetByTextOptions().setExact(true))).hasCount(2);
        assertThat(cells.getByText("Total " + year, new Locator.GetByTextOptions().setExact(true))).isVisible();
    }
}
