package ch.martinelli.edu.hrs.guestbooking;

import ch.martinelli.edu.hrs.core.ui.PlaywrightIT;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class GuestBookingIT extends PlaywrightIT {

    private static final DateTimeFormatter US_DATE = DateTimeFormatter.ofPattern("M/d/yyyy");

    @Test
    void guestSearchesBooksViewsAndCancels() {
        var today = LocalDate.now(ZoneId.of("Europe/Zurich"));
        page.navigate("http://localhost:" + localServerPort + "/booking");
        mopo.waitForConnectionToSettle();

        // 10 days ahead, so the cancellation deadline is far away
        page.getByLabel("Arrival").fill(today.plusDays(10).format(US_DATE));
        page.getByLabel("Arrival").press("Enter");
        page.getByLabel("Departure").fill(today.plusDays(12).format(US_DATE));
        page.getByLabel("Departure").press("Enter");
        button(page.locator("body"), "Search").click();
        mopo.waitForConnectionToSettle();
        assertThat(page.getByText("Double").first()).isVisible();

        button(page.locator("body"), "Book").first().click();
        mopo.waitForConnectionToSettle();
        page.getByLabel("Name").fill("Anna Müller");
        page.getByLabel("Email").fill("anna@example.com");
        page.getByLabel("Phone").fill("+41 79 123 45 67");
        // the dialog opens last, so its button is the last "Book"
        button(page.locator("body"), "Book").last().click();
        mopo.waitForConnectionToSettle();

        assertThat(page.locator("body")).containsText("Please keep your booking number");
        var matcher = Pattern.compile("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}").matcher(page.locator("body").innerText());
        org.junit.jupiter.api.Assertions.assertTrue(matcher.find(), "booking number shown");
        var bookingNumber = matcher.group();

        page.navigate("http://localhost:" + localServerPort + "/booking/reservation");
        mopo.waitForConnectionToSettle();
        page.getByLabel("Booking number").fill(bookingNumber);
        page.getByLabel("Email").fill("anna@example.com");
        button(page.locator("body"), "Search").click();
        mopo.waitForConnectionToSettle();
        assertThat(page.getByText("CONFIRMED")).isVisible();

        button(page.locator("body"), "Cancel reservation").click();
        mopo.waitForConnectionToSettle();
        button(page.locator("body"), "Yes").click();
        mopo.waitForConnectionToSettle();

        assertThat(page.getByText("CANCELLED", new Page.GetByTextOptions().setExact(true))).isVisible();
        assertThat(button(page.locator("body"), "Cancel reservation")).hasCount(0);
    }

    private Locator button(Locator scope, String name) {
        return scope.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(name).setExact(true));
    }
}
