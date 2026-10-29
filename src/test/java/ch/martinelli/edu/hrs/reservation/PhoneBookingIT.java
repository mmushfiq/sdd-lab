package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.core.ui.PlaywrightIT;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class PhoneBookingIT extends PlaywrightIT {

    @Test
    void searchBookAndSeeBookingNumber() {
        page.navigate("http://localhost:" + localServerPort + "/availability");
        mopo.waitForConnectionToSettle();

        // defaults: arrival today, departure tomorrow, 1 guest
        button("Search").click();
        mopo.waitForConnectionToSettle();
        button("Book").first().click();
        mopo.waitForConnectionToSettle();

        page.getByLabel("Name").fill("Anna Müller");
        page.getByLabel("Email").fill("anna@example.com");
        page.getByLabel("Phone").fill("+41 79 123 45 67");
        button("Save").click();
        mopo.waitForConnectionToSettle();

        assertThat(page.locator("vaadin-notification-card"))
                .containsText(Pattern.compile("Reservation confirmed: [ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}"));
    }

    private Locator button(String name) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
    }
}
