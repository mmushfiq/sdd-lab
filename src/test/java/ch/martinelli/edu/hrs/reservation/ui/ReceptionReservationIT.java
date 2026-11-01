package ch.martinelli.edu.hrs.reservation.ui;

import ch.martinelli.edu.hrs.core.ui.PlaywrightIT;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class ReceptionReservationIT extends PlaywrightIT {

    // Date format of the en-US browser locale
    private static final DateTimeFormatter BROWSER_DATE = DateTimeFormatter.ofPattern("M/d/yyyy");

    @Autowired
    DSLContext ctx;

    @AfterEach
    void deleteReservations() {
        ctx.deleteFrom(RESERVATION).execute();
    }

    private String url(String path) {
        return "http://localhost:" + localServerPort + "/" + path;
    }

    // Scenarios "Reservation created", "Overlapping reservation rejected" (room not offered) and
    // "Reservation cancelled" (room offered again)
    @Test
    void create_overlap_and_cancel_reservation_for_room_104() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        page.navigate(url("login"));
        page.getByLabel("Username").fill("reception");
        page.getByLabel("Password", new Page.GetByLabelOptions().setExact(true)).fill("reception");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log in")).click();
        page.waitForURL(url("staff"));
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Reservations")).click();
        page.waitForURL(url("reservations"));

        searchAvailability(tomorrow, tomorrow.plusDays(2), 2);
        availableRoom("104").click();
        page.getByLabel("Guest name").fill("Anna Meier");
        page.getByLabel("Email").fill("anna@example.com");
        page.locator("#create-reservation").click();

        assertThat(page.getByText(Pattern.compile("Reservation \\w{8} created"))).isVisible();
        String number = ctx.select(RESERVATION.RESERVATION_NUMBER).from(RESERVATION).fetchSingle().value1();
        assertThat(page.getByText("Reservation " + number + " created")).isVisible();
        Locator listed = page.locator("#reservations vaadin-grid-cell-content")
                .getByText(number, new Locator.GetByTextOptions().setExact(true));
        assertThat(listed).isVisible();

        searchAvailability(tomorrow.plusDays(1), tomorrow.plusDays(3), 2);
        assertThat(availableRoom("105")).isVisible();
        assertThat(availableRoom("104")).hasCount(0);
        page.locator("#close-reservation").click();
        assertThat(page.locator("vaadin-dialog")).hasCount(0);

        listed.click();
        page.locator("#cancel-reservation").click();
        page.locator("vaadin-confirm-dialog").getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Cancel reservation")).click();
        assertThat(page.getByText("Reservation " + number + " cancelled")).isVisible();

        searchAvailability(tomorrow.plusDays(1), tomorrow.plusDays(3), 2);
        assertThat(availableRoom("104")).isVisible();
    }

    private void searchAvailability(LocalDate arrival, LocalDate departure, int guests) {
        // Saving closes the dialog; wait until it is gone, then open a new one
        assertThat(page.locator("vaadin-dialog")).hasCount(0);
        page.locator("#new-reservation").click();
        fillDate("Arrival", arrival);
        fillDate("Departure", departure);
        page.getByLabel("Guests").fill(String.valueOf(guests));
        page.locator("#search-availability").click();
    }

    private void fillDate(String label, LocalDate date) {
        Locator field = page.getByLabel(label, new Page.GetByLabelOptions().setExact(true));
        field.fill(date.format(BROWSER_DATE));
        field.press("Enter");
    }

    private Locator availableRoom(String number) {
        return page.locator("#available-rooms vaadin-grid-cell-content")
                .getByText(number, new Locator.GetByTextOptions().setExact(true));
    }
}
