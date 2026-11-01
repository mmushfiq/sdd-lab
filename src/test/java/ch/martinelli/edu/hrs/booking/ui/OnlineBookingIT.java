package ch.martinelli.edu.hrs.booking.ui;

import ch.martinelli.edu.hrs.core.ui.PlaywrightIT;
import ch.martinelli.edu.hrs.reservation.ReservationService;
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
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class OnlineBookingIT extends PlaywrightIT {

    // Date format of the en-US browser locale
    private static final DateTimeFormatter BROWSER_DATE = DateTimeFormatter.ofPattern("M/d/yyyy");

    @Autowired
    DSLContext ctx;

    // No mail server in the test environment
    @MockitoBean
    JavaMailSender mailSender;

    @AfterEach
    void deleteReservations() {
        ctx.deleteFrom(RESERVATION_NIGHT).execute();
        ctx.deleteFrom(RESERVATION).execute();
    }

    // Scenarios "Guest opens the booking page", "Guest books successfully", "Reservation found",
    // "Last cancellation time shown" and "Guest cancels before the arrival date"
    @Test
    void guest_books_finds_and_cancels_without_login() {
        // Far enough ahead that the 48-hour online cancellation deadline has not passed
        LocalDate arrival = LocalDate.now(ReservationService.HOTEL_ZONE).plusDays(5);
        page.navigate("http://localhost:" + localServerPort + "/");

        fillDate("Arrival", arrival);
        fillDate("Departure", arrival.plusDays(2));
        page.getByLabel("Guests").fill("2");
        page.locator("#search-rooms").click();
        page.locator(".choose-room").first().click();
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
        assertThat(page.getByText("Status: Booked")).isVisible();
        assertThat(page.locator("#cancellation-deadline"))
                .hasText(ReservationService.cancellationText(arrival.minusDays(2).atTime(14, 0)));

        page.locator("#guest-cancel-reservation").click();
        page.locator("vaadin-confirm-dialog").getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Cancel reservation")).click();
        assertThat(page.getByText("Status: Cancelled")).isVisible();
    }

    private void fillDate(String label, LocalDate date) {
        Locator field = page.getByLabel(label, new Page.GetByLabelOptions().setExact(true));
        field.fill(date.format(BROWSER_DATE));
        field.press("Enter");
    }
}
