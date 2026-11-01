package ch.martinelli.edu.hrs.room.ui;

import ch.martinelli.edu.hrs.core.ui.PlaywrightIT;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION_NIGHT;
import static ch.martinelli.edu.hrs.db.Tables.SEASONAL_RATE;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class SeasonalRatesIT extends PlaywrightIT {

    // Date format of the en-US browser locale
    private static final DateTimeFormatter BROWSER_DATE = DateTimeFormatter.ofPattern("M/d/yyyy");

    @Autowired
    DSLContext ctx;

    // No mail server in the test environment
    @MockitoBean
    JavaMailSender mailSender;

    @AfterEach
    void cleanUp() {
        ctx.deleteFrom(RESERVATION_NIGHT).execute();
        ctx.deleteFrom(RESERVATION).execute();
        ctx.deleteFrom(SEASONAL_RATE).execute();
    }

    // Scenarios "Seasonal rate created", "Guest sees the seasonal price" and "Price per night shown to the guest"
    @Test
    void manager_sets_christmas_rate_and_guest_books_across_its_start() {
        int year = LocalDate.now().getYear() + 1;
        LocalDate christmasStart = LocalDate.of(year, 12, 20);

        page.navigate(url("login"));
        page.getByLabel("Username").fill("manager");
        page.getByLabel("Password", new Page.GetByLabelOptions().setExact(true)).fill("manager");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log in")).click();
        page.waitForURL(url("staff"));
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Seasonal rates")).click();
        page.waitForURL(url("seasonal-rates"));

        Locator roomType = page.getByLabel("Room type");
        roomType.fill("Double");
        roomType.press("Enter");
        page.getByLabel("Name", new Page.GetByLabelOptions().setExact(true)).fill("Christmas");
        fillDate("First night", christmasStart);
        fillDate("Last night", LocalDate.of(year + 1, 1, 6));
        page.getByLabel("Nightly rate").fill("180.00");
        page.locator("#save-seasonal-rate").click();
        assertThat(page.locator("#seasonal-rates vaadin-grid-cell-content")
                .getByText("CHF 180.00", new Locator.GetByTextOptions().setExact(true))).isVisible();

        // Guest: 2 nights at the base rate 140.00, 1 night at 180.00
        page.navigate(url(""));
        fillDate("Arrival", christmasStart.minusDays(2));
        fillDate("Departure", christmasStart.plusDays(1));
        page.getByLabel("Guests").fill("2");
        page.locator("#search-rooms").click();
        Locator doubleCard = page.locator(".room-card").first();
        assertThat(doubleCard).containsText("Double");
        assertThat(doubleCard).containsText("Total price: CHF 460.00");

        doubleCard.locator(".choose-room").click();
        page.getByLabel("Name", new Page.GetByLabelOptions().setExact(true)).fill("Anna Meier");
        page.getByLabel("Email").fill("anna@example.com");
        page.locator("#book").click();
        Locator number = page.locator("#reservation-number");
        assertThat(number).hasText(Pattern.compile("\\w{8}"));
        String reservationNumber = number.textContent();

        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("My reservation")).click();
        page.getByLabel("Reservation number").fill(reservationNumber);
        page.getByLabel("Email").fill("anna@example.com");
        page.locator("#find-reservation").click();
        assertThat(page.getByText("Price per night: 2 nights × CHF 140.00, 1 night × CHF 180.00")).isVisible();
        assertThat(page.getByText("Total price: CHF 460.00")).isVisible();
    }

    private String url(String path) {
        return "http://localhost:" + localServerPort + "/" + path;
    }

    private void fillDate(String label, LocalDate date) {
        Locator field = page.getByLabel(label, new Page.GetByLabelOptions().setExact(true));
        field.fill(date.format(BROWSER_DATE));
        field.press("Enter");
    }
}
